package org.ost.investigate.aws.lambda.examples.hello;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ost.investigate.aws.lambda.examples.hello.model.Message;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class DynamoDbConnector {
    @Inject
    @Named("DYNAMO_DB_TABLE")
    private String dynamoDBTable;

    @Inject
    @Named("REGION")
    private String region;

    public void putItem(Message message) {
        if (!ofNullable(message).map(Message::getText).isPresent())
            return;

        applyClient(cli -> {
            HashMap<String, AttributeValue> itemValues = new HashMap<>();
            itemValues.put("time",
                           AttributeValue.builder().s(new Timestamp(System.currentTimeMillis()).toString()).build());
            itemValues.put("message", AttributeValue.builder().s(message.getText()).build());

            PutItemRequest request = PutItemRequest.builder()
                                                   .tableName(dynamoDBTable)
                                                   .item(itemValues)
                                                   .build();
            PutItemResponse putItemResponse = cli.putItem(request);
            System.out.println("putItemResponse: " + putItemResponse.attributes());
            return null;
        });
    }

    public List<Message> getItems() {
        return applyClient(cli -> {
            ScanRequest scanRequest = ScanRequest.builder().tableName(dynamoDBTable).build();
            ScanResponse response = cli.scan(scanRequest);
            return response
                    .items()
                    .stream()
                    .map(v -> Message.builder().time(v.get("time").s()).text(v.get("message").s()).build())
                    .collect(Collectors.toList());
        });
    }


    private <T> T applyClient(Function<DynamoDbClient, T> func) {
        DynamoDbClient ddb = DynamoDbClient.builder()
                                           .region(Region.of(region))
                                           .httpClientBuilder(ApacheHttpClient.builder())
                                           .build();
        T result = func.apply(ddb);
        ddb.close();
        return result;
    }

}
