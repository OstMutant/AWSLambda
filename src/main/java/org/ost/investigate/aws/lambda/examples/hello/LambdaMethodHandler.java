package org.ost.investigate.aws.lambda.examples.hello;

import org.ost.investigate.aws.lambda.examples.hello.model.LambdaInput;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaOutput;
import org.ost.investigate.aws.lambda.examples.hello.model.Message;
import org.ost.investigate.aws.lambda.examples.hello.model.QueryParameters;

import java.util.HashMap;
import java.util.Map;

public class LambdaMethodHandler {

    private static Map<String, String> ENV = new HashMap<>(System.getenv());

    public static void main(String... args) {
        ENV.put("S3BUCKET", "s3-bucket-terra-test");
        ENV.put("S3KEY", "s3-bucket-terra-test-key");
        new LambdaMethodHandler()
                .handleRequest(LambdaInput.builder()
                                          .queryparameters(QueryParameters.builder()
                                                                          .name("TestName")
                                                                          .localisation("UA")
                                                                          .isS3ASync(true)
                                                                          .build())
                                          .httpMethod("POST")
                                          .message(Message.builder().text("Test Message").build())
                                          .build());
    }

//    public Object handleRequest(Object input) {
//        System.out.println("input - " + input);
//        return "Test - " + input.toString();
//    }

    public LambdaOutput handleRequest(LambdaInput input) {
        System.out.println("System.getenv - " + ENV);
        System.out.println("input - " + input.toString());
        if ("POST".equals(input.getHttpMethod())) {
            return post(input);
        } else {
            return get(input);
        }
    }

    private LambdaOutput post(LambdaInput input) {
        SystemEnvironmentVariables systemEnvironmentVariables = new SystemEnvironmentVariables(ENV);
        LambdaOutput lambdaOutput = new LambdaOutput();
        lambdaOutput.setGreeting(new S3().getS3Greeting(systemEnvironmentVariables.getS3BucketName(),
                                                        systemEnvironmentVariables.getS3KeyName(),
                                                        input.getQueryparameters()));
        DynamoDb dynamoDb = new DynamoDb();
        dynamoDb.putItem("messagesTable", input.getMessage());
        lambdaOutput.setMessages(dynamoDb.getItems("messagesTable"));
        System.out.println(lambdaOutput);
        return lambdaOutput;
    }

    private LambdaOutput get(LambdaInput input) {
        SystemEnvironmentVariables systemEnvironmentVariables = new SystemEnvironmentVariables(ENV);
        LambdaOutput lambdaOutput = new LambdaOutput();
        lambdaOutput.setGreeting(new S3().getS3Greeting(systemEnvironmentVariables.getS3BucketName(),
                                                        systemEnvironmentVariables.getS3KeyName(),
                                                        input.getQueryparameters()));
        lambdaOutput.setMessages(new DynamoDb().getItems("messagesTable"));
        System.out.println(lambdaOutput);
        return lambdaOutput;
    }
}
