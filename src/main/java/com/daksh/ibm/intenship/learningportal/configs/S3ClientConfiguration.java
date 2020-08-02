package com.daksh.ibm.intenship.learningportal.configs;

import com.daksh.ibm.intenship.learningportal.properties.S3ClientConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.utils.StringUtils;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
public class S3ClientConfiguration {

    @Bean
    public S3AsyncClient s3Client(S3ClientConfigurationProperties s3props,
                                  AwsCredentialsProvider credentialsProvider) {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient
                .builder().httpClient(httpClient)
                .region(s3props.getRegion())
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration);

        return s3AsyncClientBuilder.build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(S3ClientConfigurationProperties s3props) {
        if (StringUtils.isBlank(s3props.getAccessKeyId())) {
            return DefaultCredentialsProvider.create();
        } else {
            return () -> AwsSessionCredentials.create(
                    s3props.getAccessKeyId(),
                        s3props.getSecretAccessKey(),
                        s3props.getSessionToken());
        }
    }

//    public boolean putObject(String fileName, DataBuffer dataBuffer) {
//        PutObjectRequest putObjectRequest = PutObjectRequest
//                .builder()
//                .bucket(bucket)
//                .key(fileName)
//                .acl(ObjectCannedACL.PUBLIC_READ_WRITE)
//                .build();
//        PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromByteBuffer(dataBuffer.asByteBuffer()));
//        return response
//                .sdkHttpResponse()
//                .statusCode() == 200;
//    }
//
//    public boolean deleteObject(String fileName) {
//        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
//                .builder()
//                .bucket(bucket)
//                .key(fileName)
//                .build();
//        DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
//        return response
//                .sdkHttpResponse()
//                .statusCode() == 202 || response.sdkHttpResponse().statusCode() == 200
//                || response.sdkHttpResponse().statusCode() == 204;
//    }

}
