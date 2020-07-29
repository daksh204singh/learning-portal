package com.daksh.ibm.intenship.learningportal.controllers;

import com.daksh.ibm.intenship.learningportal.model.Course;
import com.daksh.ibm.intenship.learningportal.model.Lecture;
import com.daksh.ibm.intenship.learningportal.properties.S3ClientConfigurationProperties;
import com.daksh.ibm.intenship.learningportal.repository.CourseRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/courses")
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
public class CourseController {
    private final CourseRepository repository;
    private final S3AsyncClient s3AsyncClient;
    private final S3ClientConfigurationProperties s3config;

    public CourseController(CourseRepository repository, S3AsyncClient s3AsyncClient, S3ClientConfigurationProperties s3config) {
        this.repository = repository;
        this.s3AsyncClient = s3AsyncClient;
        this.s3config = s3config;
    }

    @GetMapping
    public Flux<Course> getAllCourses() {
        return repository.findAll();
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<Course>> getCourse(@PathVariable String id) {
        return repository.findById(id)
                .map(course -> ResponseEntity.ok(course))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Course> saveCourse(@RequestBody Course course) {
        return repository.save(course);
    }

    @PutMapping("{id}")
    public Mono<ResponseEntity<Course>> updateCourse(@PathVariable String id,
                                                     @RequestBody Course course) {
        return repository.findById(id)
                .flatMap(existingCourse -> {
                    existingCourse.setName(course.getId());
                    existingCourse.setPrice(course.getPrice());
                    existingCourse.setLectures(course.getLectures());
                    existingCourse.setDescription(course.getDescription());
                    existingCourse.setCreatedDate(course.getCreatedDate());
                    existingCourse.setCategories(course.getCategories());
                    return repository.save(existingCourse); })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public Mono<ResponseEntity<Void>> deleteCourse(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(course -> repository.delete(course)
                        .then(Mono.just(ResponseEntity.ok().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    @GetMapping("{id}/lectures")
    public Flux<Lecture> getAllLectures(@PathVariable String id) {
        return repository.findById(id)
                .map(course -> course.getLectures().values())
                .flux()
                .flatMap(collection -> Flux.fromIterable(collection));
    }

    @PostMapping("{id}/lectures")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<Course>> saveLecture(@PathVariable String id,
                                                    @RequestBody Lecture lecture)  {
        return repository.findById(id)
                .flatMap(course -> {
                    course.addLecture(lecture);
                    return repository.save(course)
                            .then(Mono.just(ResponseEntity.ok(course)));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}/lectures")
    public Mono<ResponseEntity<Void>> deleteAll(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(course -> {
                    course.getLectures().clear();
                    return repository.save(course)
                            .then(Mono.just(ResponseEntity.ok().<Void>build()));
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("{id}/lectures/{lectureId}")
    public Mono<ResponseEntity<?>> getLecture(@PathVariable String id,
                                              @PathVariable String lectureId) {
        return repository.findById(id)
                .map(course -> {
                    if (course.getLectures().containsKey(lectureId))
                        return ResponseEntity.ok(course.getLecture(lectureId));
                    else
                        return ResponseEntity.notFound().build();
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}/lectures/{lectureId}")
    public Mono<ResponseEntity<?>> deleteLecture(@PathVariable String id,
                                                    @PathVariable String lectureId) {
        return repository.findById(id)
                .flatMap(course -> {
                    if (course.getLectures().containsKey(lectureId)) {
                        course.getLectures().remove(lectureId);
                        return repository.save(course)
                                .then(Mono.just(ResponseEntity.ok().<Void>build()));
                    } else {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("{id}/lectures/{lectureId}")
    public Mono<ResponseEntity<?>> updateLecture(@PathVariable String id,
                                                       @PathVariable String lectureId,
                                                       @RequestBody Lecture lecture) {
        return repository.findById(id)
                .flatMap(course -> {
                    if (course.getLectures().containsKey(lectureId)) {
                        course.putLecture(lectureId, lecture);
                        return repository.save(course)
                                .then(Mono.just(ResponseEntity.ok(lecture)));
                    } else {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "{id}/lectures/{lectureId}/uploadContent",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<?>> uploadLectureContent(@PathVariable String id,
                                                        @PathVariable String lectureId,
                                                        @RequestHeader HttpHeaders headers,
                                                        @RequestPart("file") Flux<ByteBuffer> body) {
        String fileKey = UUID.randomUUID().toString();
        MediaType mediaType = headers.getContentType();

        if (mediaType == null)
            mediaType = MediaType.APPLICATION_OCTET_STREAM;

        CompletableFuture future = s3AsyncClient
                .putObject(PutObjectRequest.builder()
                        .bucket(s3config.getBucket())
                        .contentLength(headers.getContentLength())
                        .key(fileKey.toString())
                        .contentType(mediaType.toString())
                        .build(), AsyncRequestBody.fromPublisher(body));

        return Mono.fromFuture(future)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED));
    }

//    @DeleteMapping("{id}/lectures/{lectureId}/deleteContent")
//    public Mono<ResponseEntity<?>> deleteContent(@PathVariable String id,
//                                                 @PathVariable String lectureId) {
//        return repository.findById(id)
//                .flatMap(course -> {
//                    if (course.getLectures().containsKey(lectureId)) {
//                        Lecture lecture = course.getLecture(lectureId);
//                        boolean success = false;
//                        try {
//                            success = s3Util.deleteObject(lecture.getContentKey());
//                        } catch (Exception e) {
//                            System.out.println("CourseController::uploadLectureContent\n" +
//                                    "Delete from S3 failed!\n" + ">> Exception: " + e);
//                        }
//                        if (success) {
//                            lecture.setContentKey(null);
//                            lecture.setContentUrl(null);
//                            course.putLecture(lectureId, lecture);
//                            return repository.save(course)
//                                    .then(Mono.just(ResponseEntity.ok().<Void>build()));
//                        } else return Mono.just(ResponseEntity.badRequest().build());
//                    } else return Mono.just(ResponseEntity.notFound().build());
//                }).defaultIfEmpty(ResponseEntity.notFound().build());
//    }
}
