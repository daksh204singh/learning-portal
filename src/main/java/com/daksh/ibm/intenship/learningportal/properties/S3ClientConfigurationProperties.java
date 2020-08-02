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
    private String url;

    // AWS S3 requires that file parts must have at least 5MB, except
    // for the last part. This may change for other S3-compatible services, so let't
    // define a configuration property for that
    private int multipartMinPartSize = 5*1024*1024;

    public S3ClientConfigurationProperties() {
    }

    public S3ClientConfigurationProperties(String accessKeyId, String secretAccessKey, String sessionToken, Region region, String bucket, String url) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.region = region;
        this.bucket = bucket;
        this.url = url;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMultipartMinPartSize() {
        return multipartMinPartSize;
    }
}
