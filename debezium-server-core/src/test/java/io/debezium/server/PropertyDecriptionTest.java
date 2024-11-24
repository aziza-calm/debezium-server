package io.debezium.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PropertyDecryptionTest {

    @ConfigProperty(name = "jasypt.encryption.key")
    String jasyptEncryptionKey;
    @ConfigProperty(name = "example.encrypted.property")
    String configPassword;

    @Before
    void setJasyptEncryptionKey() {
        System.setProperty(CustomJasyptConfigSource.JASYPT_PASSWORD, jasyptEncryptionKey);
        System.setProperty(CustomJasyptConfigSource.JASYPT_KEY, jasyptEncryptionKey);
        System.out.println("JASYPT_PASSWORD: " + System.getenv("JASYPT_PASSWORD"));
        System.out.println("====== Decrypted config =========");
        System.out.println("Config password: " + configPassword);
    }

    @Test
    void decryptionWorks() {
        System.setProperty(CustomJasyptConfigSource.JASYPT_PASSWORD, jasyptEncryptionKey);
        assertThat(configPassword).isEqualTo("TextToEncrypt");
    }

    @Test
    void encrypPassword() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("SecretKey");
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setKeyObtentionIterations(1000);
        encryptor.setIvGenerator(new RandomIvGenerator());
        System.out.println(encryptor.encrypt("TextToEncrypt"));
    }
}
