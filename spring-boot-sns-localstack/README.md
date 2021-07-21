# Spring Boot + SNS + LocalStack

[LocalStack](https://github.com/localstack/localstack) is an open-source Python application that used to develop and test Amazon services on the local environment. It provides mock implementations of the Amazon Web Services (AWS). Also, it supports aws-cli commands in the shell. LocalStack comes with two versions: standard and pro. The standard version already provides common AWS APIs such as Lambda, SNS, SQS, S3. We can use LocalStack to test and debug our code without deploying it on the Amazon environment.

In this post, we are going to integrate Spring Boot with the Simple Notification Service (SNS) of AWS on the local environment by using the LocalStack. First, we will run LocalStack and then connect to the SNS service from Spring Boot by using the official [AWS API](https://aws.amazon.com/sdk-for-java/). Then we are going to execute a couple of SNS scenarios such as publishing a message.

Note: You can download the code as a project from [here](https://github.com/turkdogan/spring-boot-guide/tree/main/spring-boot-sns-localstack)

## Running LocalStack

We have a couple of options to run LocalStack in the local environment. Let's go with the docker-compose approach. Docker needs to be installed for this approach.

```yaml
version: '3.4'

services:
  localstack:
    image: localstack/localstack:0.12.15
    container_name: localstack_sns
    ports:
      - '4566:4566'
    environment:
      - DEFAULT_REGION=us-east-1
      - SERVICES=sns
      - DEBUG=1
      - DATA_DIR=/tmp/localstack/data
    volumes:
      - '/var/run/docker.sock:/var/run/docker.sock'
```

With a simple ```docker-compose``` command we can initialize and run the LocalStack easily. To start the LocalStack, let's run the following command in the directory of docker-compose.yml file.

```yaml
docker-compose up -d
```

In the first run, it might take some time to download the docker image, but in the consecutive ones, it will start faster. After running this command, we can use the SNS in our local operating system as if we are in the AWS environment.

## Spring Boot with SNS Integration

Amazon provides an official library for popular programming environments. In this post, we will use Java client in the Kotlin environment. Let's add SNS library to the Gradle file:

```kotlin
implementation("com.amazonaws:aws-java-sdk-sqs:1.11.970")
```

### SNS Client

Let's create client as a Spring component as shown below:

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

From now on, we can create client and do SNS-related calls by using this client. Mainly we can do the following operations by using this client component:
- Create Topic
- List Topics
- Publish Message
- Subscribe to Topic
- List Subscriptions
- Unsubscribe from Topic
- Delete Topic

### Create SNS Topic
Create an SNS topic from the client is simple. Let's create a topic as shown below:

```kotlin
val topic = "topic"
val createTopic = amazonSNS.createTopic(topic)
val topicArn = createTopic.topicArn
```

For complex scenarios, we could use ```CreateTopicRequest``` instead of providing the topic name directly. 

This code behaves similar to the ```create-topic``` command of the aws-cli tool.

```shell
aws --endpoint-url http://localhost:4566 sns create-topic --name topic
```

### List SNS Topics
Listing of SNS topics can be performed by using the ```listTopics``` method of the SNS client as represented below:

```kotlin
val request = ListTopicsRequest()
val result = amazonSNS.listTopics(request)
println(result.topics.first().topicArn)
```

In this example we are printing the ARN of the first topic. This command works similar to the ```list-topics``` command of the aws-cli. We can observe the list of topics by using the following shell command:

```shell
aws --endpoint-url http://localhost:4566 sns list-topics
```

The output of this command should give the following response:

```shell
"Topics": [
    {
        "TopicArn": "arn:aws:sns:us-east-1:000000000000:topic"
    }
]
```

### Publish SNS Message

Publish a message requires a valid topic arn.

```kotlin
val request = PublishRequest()
request.topicArn = topicArn
request.message = "this is a sample message"
request.messageGroupId = "exampleGroupId"
val result = amazonSNS.publish(request)
```

### Subscribe to SNS Topic

Subscription is the heart of the SNS. The main purpose is to direct the message to a receiver service. In this example, we are going to create a simple email subscription for a specific topi.

```kotlin
val request = SubscribeRequest()
request.protocol = "email"
request.endpoint = "example@turkdogan.dev"
request.topicArn = topicArn
val result = amazonSNS.subscribe(request)
subscriptionArn = result.subscriptionArn
```
SNS supports the following subscription protocols:
- application
- email
- firehose
- http
- https
- lambda
- sms
- sqs

Also, the same subscription could be done by using the aws-cli as shown below (note that, we are providing the arn of topic):
```shell
aws --endpoint-url http://localhost:4566 sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:topic --protocol email --notification-endpoint example@turkdogan.dev
```

The response should something like this:
```shell
{
    "SubscriptionArn": "arn:aws:sns:us-east-1:000000000000:topic:6b6dccc9-baf6-4cbb-9d40-e55d737fb0b9"
}
```

### List Subscriptions
We can use the following code to list all the subscriptions by using the SNS client:

```kotlin
val result = amazonSNS.listSubscriptions()
```

Also, we can list by following aws-cli command:

```shell
aws --endpoint-url http://localhost:4566 sns list-subscriptions
```

### Unsubscribe from SNS Topic
Unsubscribe requires a valid subscription arn.

```kotlin
request.subscriptionArn =  subscriptionArn
val result = amazonSNS.unsubscribe(request)
```

This code works similar to the following shell command (please note that we provide subscription-arn created before):
```shell
 aws --endpoint-url http://localhost:4566 sns unsubscribe --subscription-arn arn:aws:sns:us-east-1:000000000000:topic:6b6dccc9-baf6-4cbb-9d40-e55d737fb0b9
```

### Delete an SNS Topic
Like publish operation, delete also requires a valid topic arn.

```kotlin
amazonSNS.deleteTopic(topicArn)
```

Finally, we can delete a specific topic by using aws-cli command:

```shell
aws --endpoint-url http://localhost:4566 sns delete-topic --topic-arn arn:aws:sns:us-east-1:000000000000:topic
```

## Conclusion
LocalStack is a great tool to work with AWS Cloud API on the local environment. In this post, we integrated Spring Boot with LocalStack and performed common SNS scenarios. In the upcoming posts, I plan to cover SQS and S3 services.