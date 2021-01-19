package org.ost.investigate.aws.lambda.examples.hello;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.ost.investigate.aws.lambda.examples.hello.model.QueryParameters;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Optional.ofNullable;


public class S3 {
    public String getS3Greeting(String bucket, String key, QueryParameters queryParameters) {
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(key)
                .bucket(bucket)
                .build();
        System.out.println("getS3Object objectRequest - " + objectRequest);
        queryParameters = ofNullable(queryParameters).orElse(new QueryParameters());
        String messageTemplate = queryParameters.getIsASync()
                                 ? getS3ObjectAsync(objectRequest)
                                 : getS3ObjectSync(objectRequest);
        return String.format(getLocalisedMessage(messageTemplate, queryParameters.getLocalisation()),
                             queryParameters.getName() == null ? "World" : queryParameters.getName());
    }

    private String getS3ObjectAsync(GetObjectRequest objectRequest) {
        System.out.println("getS3ObjectAsync");
        S3AsyncClient s3Client = S3AsyncClient
                .builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                          .maxConcurrency(10)
                                                          .maxPendingConnectionAcquires(1000))
                .region(Region.US_EAST_2)
                .build();
        System.out.println("getS3ObjectAsync s3Client - " + s3Client);

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObject(objectRequest,
                                                                              AsyncResponseTransformer.toBytes()).get();
            System.out.println("getS3ObjectAsync objectBytes - " + objectBytes);
            String json = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
            System.out.println("getS3ObjectAsync result - " + json);
            return json;
        } catch (Exception e) {
            System.out.println("getS3ObjectAsync Exception - " + e.getMessage());
        }
        s3Client.close();
        return null;
    }

    private String getS3ObjectSync(GetObjectRequest objectRequest) {
        System.out.println("getS3ObjectSync");
        S3Client s3Client = S3Client
                .builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
        System.out.println("getS3ObjectSync s3Client - " + s3Client);
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
        System.out.println("getS3ObjectSync objectBytes - " + objectBytes);
        String json = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
        System.out.println("getS3ObjectSync result - " + json);
        s3Client.close();
        return json;
    }

    private String getLocalisedMessage(String json, String locale) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> messagesMap = gson.fromJson(json, type);
        return messagesMap.getOrDefault(locale, messagesMap.get("EN"));
    }
}
