package org.ost.investigate.aws.lambda.examples.hello;

import java.util.Map;
import java.util.stream.Collectors;

public class SystemEnvironmentVariables {
    public static String S3BUCKET = "S3BUCKET";
    public static String S3KEY = "S3KEY";

    public Map<String, String> systemEnvironmentVariables;
    public SystemEnvironmentVariables(Map<String, String> envVariables){
        systemEnvironmentVariables =
                envVariables.entrySet().stream()
                      .filter(v -> S3BUCKET.equals(v.getKey()) || S3KEY.equals(v.getKey()))
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getS3BucketName(){
        return systemEnvironmentVariables.get(S3BUCKET);
    }
    public String getS3KeyName(){
        return systemEnvironmentVariables.get(S3KEY);
    }
}
