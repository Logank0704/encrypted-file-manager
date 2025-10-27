package com.kc.encrypted.efm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${s3.enabled:false}") private boolean enabled;
    @Value("${s3.endpoint:http://localhost:9000}") private String endpoint;
    @Value("${s3.accessKey:efmadmin}") private String accessKey;
    @Value("${s3.secretKey:efmadmin123}") private String secretKey;
    @Value("${s3.region:us-east-1}") private String region;

    @Bean
    public S3Client s3Client() {
        if (!enabled) {
            log.info("S3Config: s3.enabled=false -> not creating S3Client (disk mode)");
            return null;
        }
        log.info("S3Config: creating S3Client endpoint={} region={}", endpoint, region);
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // required for MinIO
                        .build())
                .build();
    }
}
