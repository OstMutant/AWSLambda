package org.ost.investigate.aws.lambda.examples.hello;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;


public class S3 {
    public String getS3Object() {
//        return getS3ObjectAsync();
        return getS3ObjectAsync();
    }

    public String getS3ObjectAsync() {
        System.out.println("getS3Object");
        S3AsyncClient s3Client = S3AsyncClient
                .builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                          .maxConcurrency(10)
                                                          .maxPendingConnectionAcquires(1000))
                .region(Region.US_EAST_2)
                .build();
        System.out.println("getS3Object s3Client - " + s3Client);
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key("my-s3-bucket-name-terra-key")
                .bucket("my-s3-bucket-name-terra")
                .build();

        System.out.println("getS3Object objectRequest - " + objectRequest);
        ResponseBytes<GetObjectResponse> objectBytes = null;
        try {
            objectBytes = s3Client.getObject(objectRequest,
                                             AsyncResponseTransformer.toBytes()).get();
            System.out.println("getS3Object objectBytes - " + objectBytes);
            String result = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
            System.out.println("getS3Object result - " + result);
            return result;
        } catch (Exception e) {
            System.out.println("getS3Object Exception - " + e.getMessage());
        }
        s3Client.close();
        return null;
    }

    public String getS3ObjectSync() {
        System.out.println("getS3Object");

        S3Client s3Client = S3Client
                .builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
        System.out.println("getS3Object s3Client - " + s3Client);
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key("my-s3-bucket-name-terra-key")
                .bucket("my-s3-bucket-name-terra")
                .build();

        System.out.println("getS3Object objectRequest - " + objectRequest);
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
        System.out.println("getS3Object objectBytes - " + objectBytes);
        String result = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
        System.out.println("getS3Object result - " + result);
        s3Client.close();
        return result;
    }
}
