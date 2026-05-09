package com.cafe.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "media.storage", name = "enabled", havingValue = "true")
    public S3Client s3Client(MediaStorageProperties properties) {
        S3ClientBuilderFactory builderFactory = new S3ClientBuilderFactory(properties);
        return builderFactory.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "media.storage", name = "enabled", havingValue = "true")
    public ObjectStorageClient s3ObjectStorageClient(S3Client s3Client, MediaStorageProperties properties) {
        return new S3ObjectStorageClient(s3Client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient disabledObjectStorageClient() {
        return new DisabledObjectStorageClient();
    }

    private record S3ClientBuilderFactory(MediaStorageProperties properties) {
        private S3Client build() {
            software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(properties.getRegion()));

            if (properties.hasStaticCredentials()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            if (StringUtils.hasText(properties.getEndpoint())) {
                builder.endpointOverride(URI.create(properties.getEndpoint()));
            }

            if (properties.isPathStyleAccess()) {
                builder.serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
            }

            return builder.build();
        }
    }
}
