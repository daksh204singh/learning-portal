package com.daksh.ibm.intenship.learningportal.controllers;

import com.daksh.ibm.intenship.learningportal.model.Lecture;
import com.daksh.ibm.intenship.learningportal.properties.S3ClientConfigurationProperties;
import com.daksh.ibm.intenship.learningportal.repository.CourseRepository;
import com.daksh.ibm.intenship.learningportal.utils.DownloadUtility;
import com.daksh.ibm.intenship.learningportal.utils.UploadUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.transform.DeleteObjectsRequestMarshaller;

import javax.swing.text.html.Option;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.daksh.ibm.intenship.learningportal.utils.DownloadUtility.FluxResponseProvider;
import static com.daksh.ibm.intenship.learningportal.utils.UploadUtility.checkResult;

@RestController
@RequestMapping("/courses/{id}/lectures/{lectureId}")
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
@Slf4j
public class LectureController {

    private final CourseRepository courseRepository;
    private final S3AsyncClient s3AsyncClient;
    private final S3ClientConfigurationProperties s3props;
    private final UploadUtility uploadUtility;

    public LectureController(CourseRepository courseRepository, S3AsyncClient s3AsyncClient, S3ClientConfigurationProperties s3props, UploadUtility uploadUtility) {
        this.courseRepository = courseRepository;
        this.s3AsyncClient = s3AsyncClient;
        this.s3props = s3props;
        this.uploadUtility = uploadUtility;
    }

    @GetMapping
    public Mono<ResponseEntity<?>> getLecture(@PathVariable String id,
                                              @PathVariable String lectureId) {
        return courseRepository.findById(id)
                .map(course -> {
                    if (course.getLectures().containsKey(lectureId))
                        return ResponseEntity.ok(course.getLecture(lectureId));
                    else
                        return ResponseEntity.notFound().build();
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<ResponseEntity<?>> deleteLecture(@PathVariable String id,
                                                 @PathVariable String lectureId) {
        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (course.getLectures().containsKey(lectureId)) {
                        course.getLectures().remove(lectureId);
                        return deleteAllContent(id, lectureId).then(Mono.from(
                                courseRepository.save(course)
                                .thenReturn(ResponseEntity.ok().<Void>build())));
                    } else {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    public Mono<ResponseEntity<?>> updateLecture(@PathVariable String id,
                                                 @PathVariable String lectureId,
                                                 @RequestBody Lecture lecture) {
        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (course.getLectures().containsKey(lectureId)) {
                        course.putLecture(lectureId, lecture);
                        return courseRepository.save(course)
                                .then(Mono.just(ResponseEntity.ok(lecture)));
                    } else {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Single file upload.
     */
    @PostMapping("/uploadContent")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<?>> uploadLectureContent(@PathVariable String id,
                                                                   @PathVariable String lectureId,
                                                                   @RequestHeader HttpHeaders headers,
                                                                   @RequestBody Flux<ByteBuffer> body) {

        long length = headers.getContentLength();
        if (length < 0) {
            throw new UploadUtility.UploadFailedException(HttpStatus.BAD_REQUEST.value(),
                    Optional.of("required header missing: Content-length"));
        }

        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (!(course.getLectures().containsKey(lectureId))) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body("Lecture: " + lectureId + " not found!"));
                    }

                    final Lecture lecture = course.getLecture(lectureId);

                    String fileKey = UUID.randomUUID().toString();
                    MediaType mediaType = headers.getContentType();

                    if (mediaType == null)
                        mediaType = MediaType.APPLICATION_OCTET_STREAM;

                    return Mono.fromFuture(s3AsyncClient
                            .putObject(PutObjectRequest.builder()
                                    .bucket(s3props.getBucket())
                                    .contentLength(headers.getContentLength())
                                    .key(fileKey.toString())
                                    .contentType(mediaType.toString())
                                    .acl(ObjectCannedACL.PUBLIC_READ_WRITE)
                                    .build(), AsyncRequestBody.fromPublisher(body)))
                            .flatMap(response -> {
                                UploadUtility.checkResult(response);
                                lecture.putContent(fileKey,
                                        s3props.getUrl() + "/"
                                        + s3props.getBucket() + "/"
                                        + fileKey.toString());
                                course.putLecture(lectureId, lecture);
                                return courseRepository.save(course)
                                        .thenReturn(ResponseEntity
                                                .status(HttpStatus.CREATED)
                                                .body(new UploadUtility.UploadResult(HttpStatus.CREATED,
                                                        Arrays.asList(fileKey))));
                            });
                }).defaultIfEmpty(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Course: " + id + " not found!"));
    }


    /**
     * Multipart file upload
     * @param parts
     * @param headers
     * @return
     */
    @RequestMapping(value = "/uploadContent",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            method = {RequestMethod.POST, RequestMethod.PUT})
    public Mono<ResponseEntity<?>> multipartFileUpload(@PathVariable String id,
                                                       @PathVariable String lectureId,
                                                       @RequestHeader HttpHeaders headers,
                                                       @RequestBody Flux<Part> parts) {
       return courseRepository.findById(id)
               .flatMap(course -> {
                   if (!(course.getLectures().containsKey(lectureId)))
                       return Mono.just(ResponseEntity
                               .status(HttpStatus.NOT_FOUND)
                               .body("Lecture: " + lectureId + " not found!"));

                   final Lecture lecture = course.getLecture(lectureId);
                   final Map<String, String> lectureContentMap = lecture.getContentUrls();

                   return parts
                           .ofType(FilePart.class)
                           .flatMap((part) -> uploadUtility.saveFile(headers, s3props.getBucket(), part))
                           .map(key -> {
                               lectureContentMap.put(key, s3props.getUrl() +
                                       "/" + s3props.getBucket() +
                                       "/" + key);
                               return key;
                           })
                           .collect(Collectors.toList())
                           .flatMap((keys) -> {
                               course.putLecture(lectureId, lecture);
                               return courseRepository.save(course)
                                       .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                                               .body(new UploadUtility.UploadResult(HttpStatus.CREATED, keys)));
                           });
               }).defaultIfEmpty(ResponseEntity
                       .status(HttpStatus.NOT_FOUND)
                       .body("Course: " + id + " not found!"));
    }

    @GetMapping("/content/{fileKey}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable String id,
                                                               @PathVariable String lectureId,
                                                               @PathVariable String fileKey) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3props.getBucket())
                .key(fileKey)
                .build();

        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (!(course.getLectures().containsKey(lectureId)))
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Flux.empty()));

                    final Lecture lecture = course.getLecture(lectureId);
                    if (!(lecture.getContentUrls()).containsKey(fileKey))
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Flux.empty()));

                    return Mono.fromFuture(s3AsyncClient.getObject(request, new FluxResponseProvider()))
                            .map(response -> {
                                DownloadUtility.checkResult(response.sdkResponse);
                                String filename = response.sdkResponse.metadata().getOrDefault("filename", fileKey);
                                log.info("[I95] filename={}, length={}", filename, response.sdkResponse.contentLength());

                                return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_TYPE, response.sdkResponse.contentType())
                                        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(response.sdkResponse.contentLength()))
                                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                        .body(response.flux);
                            });
                });
    }

    @DeleteMapping("/content/{fileKey}")
    public Mono<ResponseEntity<Void>> deleteContent(@PathVariable String id,
                                                    @PathVariable String lectureId,
                                                    @PathVariable String fileKey) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3props.getBucket())
                .key(fileKey)
                .build();

        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (!(course.getLectures().containsKey(lectureId)))
                        return Mono.just(ResponseEntity.notFound().<Void>build());

                    final Lecture lecture = course.getLecture(lectureId);
                    if (!(lecture.getContentUrls().containsKey(fileKey)))
                        return Mono.just(ResponseEntity.notFound().<Void>build());

                    return Mono.fromFuture(s3AsyncClient.deleteObject(request))
                            .flatMap(response -> {
                                checkResult(response);
                                lecture.getContentUrls().remove(fileKey);
                                course.putLecture(lectureId, lecture);
                                return courseRepository.save(course)
                                        .thenReturn(ResponseEntity.ok().<Void>build());
                            });
                }).defaultIfEmpty(ResponseEntity.notFound().<Void>build());
    }

    @DeleteMapping("/content")
    public Mono<ResponseEntity<Void>> deleteAllContent(@PathVariable String id,
                                                       @PathVariable String lectureId) {

        return courseRepository.findById(id)
                .flatMap(course -> {
                    if (!(course.getLectures().containsKey(lectureId)))
                        return Mono.just(ResponseEntity.notFound().<Void>build());

                    final Lecture lecture = course.getLecture(lectureId);
                    if (lecture.getContentUrls().isEmpty())
                        return Mono.just(ResponseEntity.ok().<Void>build());

                    final List<ObjectIdentifier> objectIdentifiersList = lecture.getContentUrls()
                            .keySet().stream()
                            .map(key -> ObjectIdentifier.builder().key(key).build())
                            .collect(Collectors.toList());

                    Delete deleteObjects = Delete.builder()
                            .objects(objectIdentifiersList)
                            .build();

                    return Mono.fromFuture(s3AsyncClient.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(s3props.getBucket())
                            .delete(deleteObjects)
                            .build())).flatMap(response -> {
                                checkResult(response);
                                lecture.getContentUrls().clear();
                                course.putLecture(lectureId, lecture);
                                return courseRepository.save(course)
                                        .thenReturn(ResponseEntity.ok().<Void>build());
                    });
                }).defaultIfEmpty(ResponseEntity.notFound().<Void>build());
    }

    private static class DeleteFailedException extends RuntimeException {
        private static final Long serialVersionUID = 1L;

        private int statusCode;
        private Optional<String> statusText;

        public DeleteFailedException(SdkResponse response) {
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();

            if (sdkHttpResponse != null) {
                this.statusCode = sdkHttpResponse.statusCode();
                this.statusText = sdkHttpResponse.statusText();
            } else {
                this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
                this.statusText = Optional.of("UNKNOWN");
            }
        }
    }

    private static void checkResult(SdkResponse response) {
        SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
        if (sdkHttpResponse == null || !sdkHttpResponse.isSuccessful())
            Exceptions.propagate(new DeleteFailedException(response));
    }

}
