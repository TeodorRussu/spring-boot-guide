package dev.turkdogan.aws.sqs

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.SendMessageRequest
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestSQS() {

    private val queue = UUID.randomUUID().toString()

    @Autowired
    private lateinit var amazonSQS: AmazonSQSAsync

    @Autowired
    private lateinit var amazonS3Client: AmazonS3Client

    private lateinit var queueUrl: String

    private lateinit var message: Message

    @BeforeEach
    fun testCreateQueue() {
        val result = amazonSQS.createQueue(queue)
        queueUrl = result.queueUrl
    }
//
//
//    @Test
//    @Order(1)
//    fun testCreateQueue() {
//        val result = amazonSQS.createQueue(queue)
//        queueUrl = result.queueUrl
//        Assertions.assertEquals(200, result.sdkHttpMetadata.httpStatusCode)
//    }

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