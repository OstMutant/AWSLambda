package org.ost.investigate.aws.lambda.examples.hello.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryParameters {
    private String name;
    @Builder.Default
    private Boolean isS3ASync = false;
    private String localisation;
    @Builder.Default
    private Boolean isJustHello = false;
}
