package com.daksh.ibm.intenship.learningportal.controllers;

import com.daksh.ibm.intenship.learningportal.model.Course;
import com.daksh.ibm.intenship.learningportal.model.Lecture;
import com.daksh.ibm.intenship.learningportal.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseRepository repository;

    public CourseController(CourseRepository repository) {
        this.repository = repository;
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
