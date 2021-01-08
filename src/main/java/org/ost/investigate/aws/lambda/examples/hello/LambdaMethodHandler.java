package org.ost.investigate.aws.lambda.examples.hello;

public class LambdaMethodHandler {
    public static void main(String... args){
        new LambdaMethodHandler().handleRequest("Hi");
    }

    public Object handleRequest(Object input) {
        String result = "Hello World - " + input;
        System.out.println(result);
        return result;
    }
}
