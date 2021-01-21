package org.ost.investigate.aws.lambda.examples.hello;

import com.google.inject.Inject;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaInput;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaOutput;

public class RestHandler {
    @Inject
    private S3Connector s3Connector;
    @Inject
    private DynamoDbConnector dynamoDbConnector;

    public LambdaOutput post(LambdaInput input) {
        dynamoDbConnector.putItem(input.getMessage());
        return get(input);
    }

    public LambdaOutput get(LambdaInput input) {
        LambdaOutput lambdaOutput =
                LambdaOutput.builder()
                            .greeting(s3Connector.getS3Greeting(input.getQueryparameters()))
                            .messages(dynamoDbConnector.getItems())
                            .build();
        System.out.println(lambdaOutput);
        return lambdaOutput;
    }
}
