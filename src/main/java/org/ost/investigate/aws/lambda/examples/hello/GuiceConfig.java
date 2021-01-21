package org.ost.investigate.aws.lambda.examples.hello;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Map;

public class GuiceConfig extends AbstractModule {
    private Map<String, String> env;

    public GuiceConfig(Map<String, String> env) {
        this.env = env;
    }
    @Override
    protected void configure() {
        Names.bindProperties(this.binder(), this.env);
        bind(DynamoDbConnector.class).in(Singleton.class);
        bind(S3Connector.class).in(Singleton.class);
        bind(RestHandler.class).in(Singleton.class);
    }
}
