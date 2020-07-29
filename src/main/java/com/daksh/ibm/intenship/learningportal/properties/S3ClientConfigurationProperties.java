package com.daksh.ibm.intenship.learningportal.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;


@ConfigurationProperties(prefix = "aws.s3")
public class S3ClientConfigurationProperties {
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private Region region;
    private String bucket;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = Region.of(region);
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
