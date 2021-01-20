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
        queryParameters = ofNullable(queryParameters).orElse(new QueryParameters());
        String messageTemplate = queryParameters.getIsS3ASync()
                                 ? getS3ObjectAsync(bucket, key)
                                 : getS3ObjectSync(bucket, key);
        return String.format(getLocalisedMessage(messageTemplate, queryParameters.getLocalisation()),
                             queryParameters.getName() == null ? "World" : queryParameters.getName());
    }

    private String getS3ObjectAsync(String bucket, String key) {
        System.out.println("getS3ObjectAsync");
        S3AsyncClient s3Client = S3AsyncClient
                .builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                          .maxConcurrency(10)
                                                          .maxPendingConnectionAcquires(1000))
                .region(Region.US_EAST_2)
                .build();

        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(key)
                .bucket(bucket)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes =
                s3Client.getObject(objectRequest, AsyncResponseTransformer.toBytes())
                        .whenComplete((res, err) -> s3Client.close())
                        .join();

        return new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
    }

    private String getS3ObjectSync(String bucket, String key) {
        S3Client s3Client = S3Client
                .builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();

        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(key)
                .bucket(bucket)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
        String json = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
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
