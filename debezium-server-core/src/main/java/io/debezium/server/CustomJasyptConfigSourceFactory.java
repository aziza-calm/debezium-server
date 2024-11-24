package io.debezium.server;

import java.util.Collections;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class CustomJasyptConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(new CustomJasyptConfigSource());
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(110);
    }
}
