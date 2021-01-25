package org.ost.investigate.aws.lambda.examples.hello;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.SneakyThrows;
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
import java.time.Duration;
import java.util.Map;

import static java.util.Optional.ofNullable;


public class S3Connector {
    @Inject
    @Named("S3BUCKET")
    private String s3bucket;

    @Inject
    @Named("S3KEY")
    private String s3key;

    @Inject
    @Named("REGION")
    private String region;

    public String getS3Greeting(QueryParameters queryParameters) {
        queryParameters = ofNullable(queryParameters).orElse(new QueryParameters());
        String messageTemplate = queryParameters.getIsS3ASync() ? getS3ObjectAsync() : getS3ObjectSync();
        return String.format(getLocalisedMessage(messageTemplate, queryParameters.getLocalisation()),
                             queryParameters.getName() == null ? "World" : queryParameters.getName());
    }

    @SneakyThrows
    private String getS3ObjectAsync() {
        System.out.println("getS3ObjectAsync");
        S3AsyncClient s3Client = S3AsyncClient
                .builder()
                .httpClient(NettyNioAsyncHttpClient
                                    .builder()
                                    .build())
                .region(Region.of(region))
                .build();

        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(s3key)
                .bucket(s3bucket)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes =
                s3Client.getObject(objectRequest, AsyncResponseTransformer.toBytes()).get();
        String json = new String(objectBytes.asByteArray(), StandardCharsets.UTF_8);
        s3Client.close();
        return json;
    }

    private String getS3ObjectSync() {
        S3Client s3Client = S3Client
                .builder()
                .region(Region.of(region))
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();

        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(s3key)
                .bucket(s3bucket)
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
