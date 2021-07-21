package dev.turkdogan.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestSNS {

    private val topic = "topic"

    @Autowired
    private lateinit var amazonSNS: AmazonSNS

    private lateinit var topicArn : String

    private lateinit var subscriptionArn: String

    @Test
    @Order(1)
    fun testCreateTopic() {
        val createTopic = amazonSNS.createTopic(topic)
        topicArn = createTopic.topicArn
        Assertions.assertEquals(200, createTopic.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(2)
    fun testListTopics() {
        val request = ListTopicsRequest()
        val result = amazonSNS.listTopics(request)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertTrue(result.topics.isNotEmpty())
        Assertions.assertTrue(result.topics.contains(Topic().withTopicArn(topicArn)))
    }

    @Test
    @Order(3)
    fun testPublish() {
        val request = PublishRequest()
        request.topicArn = topicArn
        request.message = "this is a sample message"
        request.messageGroupId = "exampleGroupId"
        val result = amazonSNS.publish(request)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertNotNull(result.messageId)
    }

    @Test
    @Order(4)
    fun testSubscribe() {
        val request = SubscribeRequest()
        request.protocol = "email"
        request.endpoint = "example@turkdogan.dev"
        request.topicArn = topicArn
        val result = amazonSNS.subscribe(request)
        subscriptionArn = result.subscriptionArn
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(5)
    fun testListSubscriptions() {
        val result = amazonSNS.listSubscriptions()
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        Assertions.assertTrue(result.subscriptions.isNotEmpty())
        println(result.subscriptions)
        val subscription = result.subscriptions.first()
        Assertions.assertEquals("email", subscription.protocol)
        Assertions.assertEquals("example@turkdogan.dev", subscription.endpoint)
    }

    @Test
    @Order(6)
    fun testUnsubscribe() {
        val request = UnsubscribeRequest()
        request.subscriptionArn =  subscriptionArn
        val result = amazonSNS.unsubscribe(request)
        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
    }

    @Test
    @Order(7)
    fun testDeleteTopic() {
        Assertions.assertDoesNotThrow {
            val result = amazonSNS.deleteTopic(topicArn)
            Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
        }
    }
}