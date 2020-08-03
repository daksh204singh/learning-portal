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

    /**
     * Returns all the courses saved inside the CourseRepository
     * @return Flux of Course
     */
    @GetMapping
    public Flux<Course> getAllCourses() {
        return repository.findAll();
    }

    /**
     * Returns Course corresponding to id
     * @param id
     * @return Mono of ResponseEntity containing the course
     */
    @GetMapping("{id}")
    public Mono<ResponseEntity<Course>> getCourse(@PathVariable String id) {
        return repository.findById(id)
                .map(course -> ResponseEntity.ok(course))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Saves the course inside the CourseRepository
     * @param course
     * @return Mono of the course saved
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Course> saveCourse(@RequestBody Course course) {
        return repository.save(course);
    }

    /**
     * Updates a course corresponding to the id
     * @param id
     * @param course
     * @return Mono of ResponseEntity containing the updated course
     */
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

    /**
     * Deletes the course corresponding to the id from the CourseRepository
     * @param id
     * @return Mono of ResponseEntity<Void> containing the response of the request
     */
    @DeleteMapping("{id}")
    public Mono<ResponseEntity<Void>> deleteCourse(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(course -> repository.delete(course)
                        .then(Mono.just(ResponseEntity.ok().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * deletes all courses stored inside the CourseRepository
     * @return Mono<Void> signalling successful execution
     */
    @DeleteMapping
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    /**
     * Returns all lectures associated with the Course of the specified id
     * @param id
     * @return Flux of Lecture objects
     */
    @GetMapping("{id}/lectures")
    public Flux<Lecture> getAllLectures(@PathVariable String id) {
        return repository.findById(id)
                .map(course -> course.getLectures().values())
                .flux()
                .flatMap(collection -> Flux.fromIterable(collection));
    }

    /**
     * saves lecture inside course and updates course in the CourseRepository
     * @param id
     * @param lecture
     * @return Mono of ResponseEntity containing the updated course as body
     */
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

    /**
     * Delets all lectures from the course with the id specified
     * @param id
     * @return Mono of ResponseEntity with body void signaling successful execution
     */
    @DeleteMapping("{id}/lectures")
    public Mono<ResponseEntity<Void>> deleteAll(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(course -> {
                    course.getLectures().clear();
                    return repository.save(course)
                            .then(Mono.just(ResponseEntity.ok().<Void>build()));
                }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

}
