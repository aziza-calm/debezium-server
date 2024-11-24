/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.server;

import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {

    public static void main(String... args) {
        Quarkus.run(args);
        ServiceLoader<ConfigSource> loader = ServiceLoader.load(ConfigSource.class);
        for (ConfigSource source : loader) {
            System.out.println("Loaded ConfigSource: " + source.getName());
        }

    }

}
