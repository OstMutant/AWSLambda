package org.ost.investigate.aws.lambda.examples.hello;

import org.ost.investigate.aws.lambda.examples.hello.model.LambdaInput;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaOutput;
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
                                                                          .isASync(true)
                                                                          .build())
                                          .message("Test Message")
                                          .build());
    }

    public LambdaOutput handleRequest(LambdaInput input) {
        System.out.println("System.getenv - " + ENV);
        SystemEnvironmentVariables systemEnvironmentVariables = new SystemEnvironmentVariables(ENV);
        LambdaOutput lambdaOutput = new LambdaOutput();
        lambdaOutput.setGreeting(new S3().getS3Greeting(systemEnvironmentVariables.getS3BucketName(),
                                                        systemEnvironmentVariables.getS3KeyName(),
                                                        input.getQueryparameters()));
        System.out.println(lambdaOutput);
        return lambdaOutput;
    }
}
