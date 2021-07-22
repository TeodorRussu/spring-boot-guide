# Spring Boot + SNS + SQS + LocalStack

In the [previous](https://turkdogan.medium.com/spring-boot-sns-localstack-619b9b75f2ac) post, we set up a Spring Boot project and performed a couple of Amazon Simple Notification Service (SNS) tasks. In this post, we will continue to integrate Amazon Simple Queue Service (SQS). First, we will develop a client to connect Amazon SQS. Then, we will create queues by using this client implementation. In the final step, we are going to subscribe to the Amazon SNS to redirect a specific message to the corresponding queue.

Note: You can download the code as a Kotlin+Gradle project from [here](https://github.com/turkdogan/spring-boot-guide/tree/main/spring-boot-sns-sqs-localstack)

## Running LocalStack

We need to add Amazon SNS and SQS in the docker-compose file. 

```yaml
@Configuration
class AWSSQSConfig {

  @Bean(destroyMethod = "shutdown")
  fun amazonSQS(): AmazonSQSAsync {
    return AmazonSQSAsyncClient.asyncBuilder()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(
                    "http://localhost:4566", "us-east-1"))
            .withCredentials(AWSStaticCredentialsProvider(
                    BasicAWSCredentials("foo", "bar")))
            .build()
  }
```

Let's start the LocalStack service.

```yaml
docker-compose up -d
```

From now on, we can test our services by using LocalStack.

## Spring Boot with SQS Integration

### SQS Client

We need the SQS library provided by Amazon. We can connect to Amazon SQS and perform SQS related operations. Let's create a Spring SQS client component.

```kotlin
@Configuration
class AWSSNSConfig {

    @Bean(destroyMethod = "shutdown")
    fun amazonSNS(): AmazonSNS {
        return AmazonSNSClient.builder()
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(
                        "http://localhost:4566", "us-east-1"))
                .withCredentials(AWSStaticCredentialsProvider(
                        BasicAWSCredentials("foo", "bar")))
                .build()
    }
}
```

LocalStack uses 4566 port to communicate with all the AWS services. We do not need to provide any real credentials to be able to communicate with LocalStack.

### Common SQS Scenarios
Let's do a couple of SQS tasks in Spring Boot:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestSQS() {

    private val queue = UUID.randomUUID().toString()

    @Autowired
    private lateinit var amazonSQS: AmazonSQSAsync

    private lateinit var queueUrl: String

    private lateinit var message: Message

    @Test
    @Order(1)
    fun testCreateQueue() {
        val result = amazonSQS.createQueue(queue)
        queueUrl = result.queueUrl
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(1)
    fun testCreateFifoQueue() {
        val request = CreateQueueRequest()
        request.queueName = "$queue.fifo"
        request.addAttributesEntry("FifoQueue", "true")
        val result = amazonSQS.createQueue(request)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(2)
    fun testListQueues() {
        val result = amazonSQS.listQueues()
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertTrue(result.queueUrls.isNotEmpty())
        Assertions.assertTrue(result.queueUrls.contains(queueUrl))
    }

    @Test
    @Order(3)
    fun testSendMessage() {
        val request = SendMessageRequest()
        request.messageBody = "This is SQS message"
        request.queueUrl = queueUrl
        val result = amazonSQS.sendMessage(request)
        val messageId = result.messageId
        Assertions.assertNotNull(messageId)
        val receiveMessageResult = amazonSQS.receiveMessage(queueUrl)
        message = receiveMessageResult.messages.first()
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertEquals(request.messageBody, message.body)
        Assertions.assertEquals(messageId, message.messageId)
    }

    @Test
    @Order(4)
    fun testDeleteMessage() {
        val request = DeleteMessageRequest()
        request.queueUrl = queueUrl
        request.receiptHandle = message.receiptHandle
        val result = amazonSQS.deleteMessage(request)
        val receiveMessageResult = amazonSQS.receiveMessage(queueUrl)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertTrue(receiveMessageResult.messages.isNullOrEmpty())
    }

    @Test
    @Order(5)
    fun testDeleteQueue() {
        val result = amazonSQS.deleteQueue(queueUrl)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }
}
```

Please note that we run the tests in a specific order. In the first test, we are creating an SQS queue. In the second test, we are retrieving the queues created in the Amazon SQS. We should expect to see the queue created in the first test. Then we are sending to the queue and reading this message from the queue. In the last two test scenarios, we are deleting the queue message and the queue.

## SQS and SNS Integration
In this part, we are going to integrate SNS and SQS. Whenever we send an SNS message, the subscribed queue should receive that message. Below the whole test file is represented:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestSQSIntegration {

    private val topic = "topic"

    private val queue1 = UUID.randomUUID().toString()

    private val queue2 = UUID.randomUUID().toString()

    @Autowired
    private lateinit var amazonSNS: AmazonSNS

    @Autowired
    private lateinit var amazonSQS: AmazonSQSAsync

    private lateinit var topicArn : String

    private lateinit var queueUrl1: String
    private lateinit var queueUrl2: String

    @Test
    @Order(1)
    fun testCreateTopic() {
        val createTopic = amazonSNS.createTopic(topic)
        topicArn = createTopic.topicArn
        Assertions.assertEquals(200, createTopic.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(2)
    fun testCreateQueues() {
        var result = amazonSQS.createQueue(queue1)
        queueUrl1 = result.queueUrl
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)

        result = amazonSQS.createQueue(queue2)
        queueUrl2 = result.queueUrl
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(3)
    fun testSubscriptions() {
        // first queue
        var subscribeQueue = Topics.subscribeQueue(amazonSNS, amazonSQS, topicArn, queueUrl1)
        Assertions.assertTrue(subscribeQueue.contains(topic))

        // second queue
        subscribeQueue = Topics.subscribeQueue(amazonSNS, amazonSQS, topicArn, queueUrl2)
        Assertions.assertTrue(subscribeQueue.contains(topic))
    }

    @Test
    @Order(4)
    fun testPublish() {
        val request = PublishRequest()
        request.topicArn = topicArn
        request.subject = "This is a sample subject"
        request.message = "This foo is a sample message"
        request.messageGroupId = "ExampleGroupId"
        val result = amazonSNS.publish(request)

        val receiveMessageResult1 = amazonSQS.receiveMessage(
                ReceiveMessageRequest()
                        .withWaitTimeSeconds(5)
                        .withQueueUrl(queueUrl1)
        )

        val receiveMessageResult2 = amazonSQS.receiveMessage(
                ReceiveMessageRequest()
                        .withWaitTimeSeconds(5)
                        .withQueueUrl(queueUrl2)
        )

        val objectMapper = ObjectMapper()

        val message1 = receiveMessageResult1.messages.first()
        val bodyMap1 = objectMapper.readValue(message1.body, Map::class.java)

        val message2 = receiveMessageResult2.messages.first()
        val bodyMap2 = objectMapper.readValue(message2.body, Map::class.java)

        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertNotNull(result.messageId)

        Assertions.assertTrue(receiveMessageResult1.messages.isNotEmpty())
        Assertions.assertEquals(request.message, bodyMap1["Message"])
        Assertions.assertEquals(topicArn, bodyMap1["TopicArn"])
        Assertions.assertEquals(request.subject, bodyMap1["Subject"])

        Assertions.assertTrue(receiveMessageResult2.messages.isNotEmpty())
        Assertions.assertEquals(request.message, bodyMap2["Message"])
        Assertions.assertEquals(topicArn, bodyMap2["TopicArn"])
        Assertions.assertEquals(request.subject, bodyMap2["Subject"])
    }
}
```
Let's explain this code function by function. As in the first test file, tests in this one also works in the specified order. In the first test, we are creating an SNS topic. Then in the second one, we are creating two Amazon SQS definitions. In the third test, we subscribe queues to the Amazon SNS topic. 

In the last test scenario, we send an SNS message and expect both of the queues to receive that message. The message content is sent in JSON format. Below you see a typical SQS message in JSON format:

```json
{
   "MessageId":"4e68039b-faaf-4aba-67da-fe75cfcb0b4b",
   "ReceiptHandle":"xxwcwycayeifjskwiqribwcxsaxjbbsoqtwidvunpekzjhxwppszbufvvecxoyaexylajcpmyrdibhlqdjdoyfjeqihvuwkrdjhermstvyblisrhpswpznglwhcesbnskcyxymvonfyzjtykkoikyasnksafaegwsvdlkaiptjmrihzuduyzprdmg",
   "MD5OfBody":"2cb3e645c71bf662f72fe8ca5b5c5d12",
   "Body":{
      "Type":"Notification",
      "MessageId":"9774de9f-1677-4ac9-a35d-95cce390ab5a",
      "TopicArn":"arn:aws:sns:us-east-1:000000000000:topic",
      "Message":"This foo is a sample message",
      "Timestamp":"2021-07-22T09:04:00.495Z",
      "SignatureVersion":"1",
      "Signature":"EXAMPLEpH+..",
      "SigningCertURL":"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-0000000000000000000000.pem",
      "Subject":"This is a sample subject"
   },
   "Attributes":{
      
   },
   "MessageAttributes":{
      
   }
}
```

In the last part of the test, we have converted the JSON value to a map to be able to retrieve details of the message. Then we verified that the message retrieved by queues has correct content.

## SNS Message Filtering
Until now, when we send an SNS message, all the subscribed services retrieve that message. It is possible redirect messages only to the relevant receiver without blocking unrelated queues. Amazon SNS provides message filtering support to differentiate the receivers of the messages. Filter policy is a JSON object to map specific policy content to the corresponding queue.

### Add Filter Policy to SNS

Adding filter policy requires a valid subscription ARN. 

```kotlin
var subscriptionArn = Topics.subscribeQueue(amazonSNS, amazonSQS, topicArn, queueUrl1)
Assertions.assertTrue(subscriptionArn.contains(topic))

var filterPolicyString = "{\"event\":[\"${filterPolicy1}\"]}"
var request = SetSubscriptionAttributesRequest(subscriptionArn, "FilterPolicy", filterPolicyString)
amazonSNS.setSubscriptionAttributes(request)
```

Please note that we must provide policy content by using the ```FilterPolicy``` attribute name.

### Sending SNS Message with Policy

To be able to add policy messages, we have to provide policy content as a message attribute. In the example below, the filter policy contains an ```event``` key attribute with a value. This value must match with the one we provided during the queue subscription to the topic. 

```kotlin
@Test
@Order(4)
fun testRedirectToFirstQueueOnly() {
    val request = PublishRequest()
    request.topicArn = topicArn
    request.subject = "This is a sample subject"
    request.message = "This foo is a sample message"
    request.messageGroupId = "ExampleGroupId"

    val messageAttributeValue = MessageAttributeValue().withDataType("String.Array")
            .withStringValue("[\"$filterPolicy1\"]")
    request.addMessageAttributesEntry("event", messageAttributeValue)

    val result = amazonSNS.publish(request)

    val receiveMessageResult1 = amazonSQS.receiveMessage(
            ReceiveMessageRequest()
                    .withWaitTimeSeconds(5)
                    .withQueueUrl(queueUrl1)
    )

    val receiveMessageResult2 = amazonSQS.receiveMessage(
            ReceiveMessageRequest()
                    .withWaitTimeSeconds(5)
                    .withQueueUrl(queueUrl2)
    )

    val objectMapper = ObjectMapper()

    val message1 = receiveMessageResult1.messages.first()
    val bodyMap1 = objectMapper.readValue(message1.body, Map::class.java)

    Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    Assertions.assertNotNull(result.messageId)

    Assertions.assertTrue(receiveMessageResult1.messages.isNotEmpty())
    Assertions.assertEquals(request.message, bodyMap1["Message"])
    Assertions.assertEquals(topicArn, bodyMap1["TopicArn"])
    Assertions.assertEquals(request.subject, bodyMap1["Subject"])

    Assertions.assertTrue(receiveMessageResult2.messages.isEmpty())
}
```
When we run this test scenario, we should observe that only the first queue must retrieve the message.

## Summary
In this post, we tested a couple of Amazon SQS tasks such as creating a queue, sending a message. Also, we integrated the Amazon SQS and SNS topic and redirected messages to the corresponding queues. Thanks to the LocalStack, we have tested all implementations offline without connecting Amazon Cloud.