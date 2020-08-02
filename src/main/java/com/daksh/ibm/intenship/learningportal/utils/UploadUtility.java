package com.daksh.ibm.intenship.learningportal.utils;

import com.daksh.ibm.intenship.learningportal.properties.S3ClientConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Upload
 */
@Component
@Slf4j
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
public class UploadUtility {

    private S3AsyncClient s3AsyncClient;

    private S3ClientConfigurationProperties s3props;

    public UploadUtility(S3AsyncClient s3AsyncClient, S3ClientConfigurationProperties s3props) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3props = s3props;
    }

    /**
     * check result from an API call.
     * @param result Result from an API call
     */
    public static void checkResult(SdkResponse result) {
        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful())
            throw Exceptions.propagate(new UploadFailedException(result));
    }

    /**
     * Upload a single file part to the requested bucket
     * @param uploadState
     * @param buffer
     * @return
     */
    public Mono<CompletedPart> uploadPart(UploadState uploadState, ByteBuffer buffer) {
        final int partNumber = ++uploadState.partCounter;
        log.info("[I218] uploadPart: partNumber={}, contentLength={}", partNumber, buffer.capacity());

        CompletableFuture<UploadPartResponse> request = s3AsyncClient.uploadPart(UploadPartRequest.builder()
                .bucket(s3props.getBucket())
                .key(uploadState.getFileKey())
                .partNumber(partNumber)
                .uploadId(uploadState.uploadId)
                .contentLength((long) buffer.capacity())
                .build(), AsyncRequestBody.fromByteBuffer(buffer));

        return Mono
                .fromFuture(request)
                .map(uploadPartResult -> {
                    checkResult(uploadPartResult);
                    log.info("[I230] uploadPart complete: part={}, etags={}", partNumber, uploadPartResult.eTag());
                    return CompletedPart.builder()
                            .eTag(uploadPartResult.eTag())
                            .partNumber(partNumber)
                            .build();
                });
    }

    public Mono<CompleteMultipartUploadResponse> completeUpload(UploadState state) {
        log.info("[I202] completeUpload: bucket={}, fileKey={}, completedParts.size={}", state.getBucket(),
                state.getFileKey(), state.completedParts.size());

        CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
                .parts(state.completedParts.values())
                .build();

        return Mono.fromFuture(s3AsyncClient.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(state.getBucket())
                .uploadId(state.uploadId)
                .multipartUpload(multipartUpload)
                .key(state.getFileKey())
                .build()));
    }

    /**
     * Save file using a multipart upload. This method does not require any temporary
     * storage at the REST service.
     * @param headers
     * @param bucket Bucket name
     * @param filePart Uploaded file
     * @return
     */
    public Mono<String> saveFile(HttpHeaders headers, String bucket, FilePart filePart) {

        // Generate a file key for this upload
        String fileKey = UUID.randomUUID().toString();

        // Gather metadata
        Map<String, String> metadata = new HashMap<>();
        String filename = filePart.filename();
        if (filename == null)
            filename = fileKey;

        metadata.put("filename", filename);

        MediaType mediaType = filePart.headers().getContentType();
        if (mediaType == null)
            mediaType = MediaType.APPLICATION_OCTET_STREAM;

        // Create a multipart upload request
        CompletableFuture<CreateMultipartUploadResponse> uploadRequest = s3AsyncClient
                .createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .contentType(mediaType.toString())
                        .key(fileKey)
                        .metadata(metadata)
                        .bucket(bucket)
                        .acl(ObjectCannedACL.PUBLIC_READ_WRITE)
                        .build());

        // This variable will hold the upload state that we must keep
        // around until all uploads complete
        final UploadState uploadState = new UploadState(bucket, fileKey);

        return Mono.fromFuture(uploadRequest)
                .flatMapMany((response) -> {
                    checkResult(response);
                    uploadState.uploadId = response.uploadId();
                    log.info("[I183] uploadId={}", response.uploadId());
                    return filePart.content();
                }).bufferUntil(buffer -> {
                    uploadState.buffered += buffer.readableByteCount();
                    if (uploadState.buffered >= s3props.getMultipartMinPartSize()) {
                        log.info("[I173] bufferUntil: returning true, " +
                                "bufferedBytes={}, partCounter={}, " +
                                "uploadId={}", uploadState.buffered,
                                uploadState.partCounter, uploadState.uploadId);
                        uploadState.buffered = 0; // reset buffer
                        return true;
                    } else {
                        return false;
                    }
                }).map(Utility::concatBuffers)
                .flatMap(buffer -> uploadPart(uploadState, buffer))
                .onBackpressureBuffer()
                .reduce(uploadState, (state, completedPart) -> {
                    log.info("[I188] completed: partNumber={}, etag{}",
                            completedPart.partNumber(), completedPart.eTag());
                    state.completedParts.put(completedPart.partNumber(), completedPart);
                    return state;
                })
                .flatMap(state -> completeUpload(state))
                .map(response -> {
                    checkResult(response);
                    return uploadState.getFileKey();
                });
    }

    public static class UploadFailedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private int statusCode;
        private Optional<String> statusText;

        public UploadFailedException(SdkResponse response) {

            SdkHttpResponse httpResponse = response.sdkHttpResponse();
            if (httpResponse != null) {
                this.statusCode = httpResponse.statusCode();
                this.statusText = httpResponse.statusText();
            } else {
                this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
                this.statusText = Optional.of("UNKNOWN");
            }
        }

        public UploadFailedException(int statusCode, Optional<String> statusText) {
            this.statusCode = statusCode;
            this.statusText = statusText;
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class UploadResult {
        HttpStatus status;
        String[] keys;

        public UploadResult() {}

        public UploadResult(HttpStatus status, List<String> keys) {
            this.status = status;
            this.keys = keys == null ? new String[] {} : keys.toArray(new String[] {});
        }
    }

    public static class UploadState {
        private final String bucket;
        private final String fileKey;

        public String uploadId;
        public int partCounter;
        public Map<Integer, CompletedPart> completedParts = new HashMap<>();
        public int buffered = 0;

        public UploadState(String bucket, String fileKey) {
            this.bucket = bucket;
            this.fileKey = fileKey;
        }

        public String getBucket() {
            return bucket;
        }

        public String getFileKey() {
            return fileKey;
        }

    }
}
