package io.debezium.server;

import static io.smallrye.config._private.ConfigLogging.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.EncryptableProperties;
import org.jasypt.util.text.BasicTextEncryptor;

import lombok.Value;

public class CustomJasyptConfigSource implements ConfigSource {

    static {
        System.out.println("CustomJasyptConfigSource class loaded by: " + CustomJasyptConfigSource.class.getClassLoader());
    }

    public static final String JASYPT_PASSWORD = "jasypt.password";
    public static final String JASYPT_KEY = "jasypt.key";
    public static final String JASYPT_PROPERTIES = "jasypt.properties";

    private static final Pattern PATTERN = Pattern.compile("[^a-zA-Z0-9_]");
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String DECRYPTION_FAILURE_MESSAGE = "Could not decrypt property {}; falling back to unencrypted property";

    private Properties properties = new Properties();
    private final EncryptableProperties encryptableProperties;
    private final String propertyFilename;

    public CustomJasyptConfigSource() {
        log.info("=========== Loading using CustomJasyptConfigSource");
        final PropertiesAndName propertiesAndName = loadProperties();
        this.properties = propertiesAndName.getProperties();
        this.propertyFilename = propertiesAndName.getFilename();
        this.encryptableProperties = new EncryptableProperties(properties, getEncryptor());
    }

    @Override
    public String getName() {
        return "CustomJasyptConfigSource";
    }

    /**
     * Config source priority. Chosen to be higher than Quarkus application.properties (250), but lower than
     * Eclipse Microprofile EnvConfigSource (300).
     */
    @Override
    public int getOrdinal() {
        return 270;
    }

    protected String property(String propertyName, Supplier<String> defaultValue) {
        String envVarName = envVarName(propertyName);
        return Optional.ofNullable(System.getenv(envVarName))
                .orElseGet(
                        () -> Optional.ofNullable(System.getProperty(propertyName))
                                .orElseGet(() -> Optional.ofNullable(properties.getProperty(propertyName))
                                        .orElse(defaultValue.get())));
    }

    protected String property(String propertyName, String defaultValue) {
        return property(propertyName, () -> defaultValue);
    }

    protected String envVarName(String propertyName) {
        return PATTERN.matcher(propertyName).replaceAll("_").toUpperCase();
    }

    protected BasicTextEncryptor getEncryptor() {
        return createStringEncryptor();
    }

    protected BasicTextEncryptor createStringEncryptor() {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(property(JASYPT_PASSWORD, () -> property(JASYPT_KEY, getDefaultPassword())));
        return encryptor;
    }

    /**
     * Default Jasypt encryption password. Override this if a custom password resolution strategy is desired.
     */
    protected String getDefaultPassword() {
        // The non-empty default password defined here is not supposed to be used for encryption, but simplifies instantiating
        // this class if no password is set, e.g. as part of the Quarkus build-time property resolution.
        return " ";
    }

    /**
     * Comma-separated property filenames, resolved from filesystem or classpath if prefixed with <code>classpath:</code>.
     */
    protected String getCommaSeparatedPropertyFilenames() {
        return property(JASYPT_PROPERTIES, "classpath:application.properties,config/application.properties");
    }

    @Value
    class PropertiesAndName {
        Properties properties;
        String filename;

        public PropertiesAndName(Properties properties, String filename) {
            this.properties = properties;
            this.filename = filename;
        }

        public Properties getProperties() {
            return properties;
        }

        public String getFilename() {
            return filename;
        }
    }

    protected PropertiesAndName loadProperties() {
        final List<String> propertyFilenames = Arrays.asList(getCommaSeparatedPropertyFilenames().split(","));
        for (final String propertyFilename : propertyFilenames) {
            log.info("Trying to load properties from " + propertyFilename);
            try (final InputStream is = createInputStream(propertyFilename)) {
                return createProperties(propertyFilename, is);
            }
            catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.trace("Could not open input stream for {}", propertyFilename, e);
                }
                else {
                    log.info("Could not open input stream for " + propertyFilename);
                }
            }
        }
        log.warn("Could not read properties from any file in " + propertyFilenames);
        return new PropertiesAndName(new Properties(), "n/a");
    }

    private PropertiesAndName createProperties(String propertyFilename, InputStream is) throws IOException {
        final Properties properties = new Properties();
        properties.load(is);
        log.info("Loaded " + properties.size() + " " + (properties.size() == 1 ? "property" : "properties") + " from " + propertyFilename);
        return new PropertiesAndName(properties, propertyFilename);
    }

    private InputStream createInputStream(String location) throws Exception {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(location.substring(CLASSPATH_PREFIX.length()));
        }
        else {
            return new FileInputStream(new File(location));
        }
    }

    @Override
    public Map<String, String> getProperties() {
        final Map<String, String> propertyMap = new HashMap<>();
        for (final String name : encryptableProperties.stringPropertyNames()) {
            propertyMap.put(name, getValue(name));
        }
        return propertyMap;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.stringPropertyNames();
    }

    @Override
    public String getValue(String key) {
        try {
            return encryptableProperties.getProperty(key);
        }
        catch (EncryptionOperationNotPossibleException e) {
            if (log.isDebugEnabled()) {
                log.debug(DECRYPTION_FAILURE_MESSAGE, key, e);
            }
            else {
                log.warn(DECRYPTION_FAILURE_MESSAGE, key, e);
            }
            return properties.getProperty(key);
        }
    }

    public String toString() {
        return getName();
    }
}
