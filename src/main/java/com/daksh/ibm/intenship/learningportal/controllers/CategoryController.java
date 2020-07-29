package com.daksh.ibm.intenship.learningportal.controllers;

import com.daksh.ibm.intenship.learningportal.model.Category;
import com.daksh.ibm.intenship.learningportal.model.Course;
import com.daksh.ibm.intenship.learningportal.repository.CategoryRepository;
import com.daksh.ibm.intenship.learningportal.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private CategoryRepository repository;
    private CourseRepository courseRepository;

    public CategoryController(CategoryRepository repository, CourseRepository courseRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public Flux<Category> getAllCategories() {
        return repository.findAll();
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<Category>> getCategory(@PathVariable String id) {
        return repository.findById(id)
                .map(category -> ResponseEntity.ok(category))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Category> saveCategory(@RequestBody Category category) {
        return repository.save(category);
    }

    @PutMapping("{id}")
    public Mono<ResponseEntity<Category>> updateProduct(@PathVariable String id,
                                                        @RequestBody Category category) {
        return repository.findById(id)
                .flatMap(existingCategory ->
                        repository.save(new Category(existingCategory.getId(),
                                category.getName(),
                                category.getCourseList())))
                .map(updateProduct -> ResponseEntity.ok(updateProduct))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @DeleteMapping("{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(existingCategory ->
                        repository.delete(existingCategory)
                                .then(Mono.just(ResponseEntity.ok().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<Category>> addCourse(@PathVariable String id, @RequestBody Course course) {
        return repository.findById(id)
                .flatMap(category -> {
                    course.getCategories().add(category.getId());
                    return courseRepository.save(course)
                            .flatMap(savedCourse -> {
                                category.getCourseList().add(savedCourse.getId());
                                return repository.save(category)
                                .map(updatedCategory -> ResponseEntity.ok(updatedCategory));
                            });
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @GetMapping("{id}/courses")
    public Flux<Course> getCourses(@PathVariable String id) {
        return repository.findById(id)
                .map(category -> category.getCourseList())
                .flux()
                .flatMap(list -> courseRepository.findAllById(list));
    }
}
