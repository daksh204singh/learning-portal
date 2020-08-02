package com.daksh.ibm.intenship.learningportal.utils;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class DownloadUtility {

    /**
     * Holds the API response and stream
     */
    public static class FluxResponse {
        public final CompletableFuture<FluxResponse> cf = new CompletableFuture<>();
        public GetObjectResponse sdkResponse;
        public Flux<ByteBuffer> flux;
    }

    @AllArgsConstructor
    public static class DownloadFailedException extends RuntimeException {

        private static final Long serialVersionUID = 1L;

        private int statusCode;
        private Optional<String> statusText;

        public DownloadFailedException(SdkResponse response) {
            SdkHttpResponse httpResponse = response.sdkHttpResponse();

            if (httpResponse != null) {
                this.statusCode =  httpResponse.statusCode();
                this.statusText = httpResponse.statusText();
            } else {
                this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
                this.statusText = Optional.of("UNKNOWN");
            }
        }

    }

    /**
     * check result from an API call.
     * @param result Result from an API call
     */
    public static void checkResult(SdkResponse result) {
        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful())
            throw Exceptions.propagate(new DownloadFailedException(result));
    }

    public static class FluxResponseProvider implements AsyncResponseTransformer<GetObjectResponse,FluxResponse> {

        private FluxResponse response;

        @Override
        public CompletableFuture<FluxResponse> prepare() {
            response = new FluxResponse();
            return response.cf;
        }

        @Override
        public void onResponse(GetObjectResponse sdkResponse) {
            this.response.sdkResponse = sdkResponse;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            response.flux = Flux.from(publisher);
            response.cf.complete(response);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            response.cf.completeExceptionally(error);
        }
    }

}
