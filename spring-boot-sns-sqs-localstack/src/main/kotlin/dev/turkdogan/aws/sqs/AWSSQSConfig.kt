package dev.turkdogan.aws.sqs

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
}
