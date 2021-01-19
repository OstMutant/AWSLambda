package org.ost.investigate.aws.lambda.examples.hello.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LambdaOutput {
    private String greeting;
    private String messages;
}
