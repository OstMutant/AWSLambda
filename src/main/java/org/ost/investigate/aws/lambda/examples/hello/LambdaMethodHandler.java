package org.ost.investigate.aws.lambda.examples.hello;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaInput;
import org.ost.investigate.aws.lambda.examples.hello.model.LambdaOutput;
import org.ost.investigate.aws.lambda.examples.hello.model.Message;
import org.ost.investigate.aws.lambda.examples.hello.model.QueryParameters;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class LambdaMethodHandler {

    private static Map<String, String> ENV = new HashMap<>(System.getenv());

    public static void main(String... args) {
        ENV.put("S3BUCKET", "s3-bucket-terra-test");
        ENV.put("S3KEY", "s3-bucket-terra-test-key");
        ENV.put("DYNAMO_DB_TABLE", "messagesTable");
        ENV.put("REGION", "us-east-2");
        new LambdaMethodHandler()
                .handleRequest(LambdaInput.builder()
                                          .queryparameters(QueryParameters.builder()
                                                                          .name("TestName")
                                                                          .localisation("UA")
                                                                          .isS3ASync(true)
                                                                          .build())
                                          .httpMethod("GET")
                                          .message(Message.builder().text("Test Message").build())
                                          .build());
    }

//    public Object handleRequest(Object input) {
//        System.out.println("input - " + input);
//        return "Test - " + input.toString();
//    }

    public LambdaOutput handleRequest(LambdaInput input) {
        Injector injector = Guice.createInjector(new GuiceConfig(ENV));
        RestHandler restHandler = injector.getInstance(RestHandler.class);
        System.out.println("System.getenv - " + ENV);
        System.out.println("input - " + input.toString());
        if(ofNullable(input.getQueryparameters()).filter(QueryParameters::getIsJustHello).isPresent()){
            return LambdaOutput.builder().greeting("Just Hello World!").build();
        }

        if ("POST".equals(input.getHttpMethod())) {
            return restHandler.post(input);
        } else {
            return restHandler.get(input);
        }
    }
}
