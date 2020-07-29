package com.daksh.ibm.intenship.learningportal.repository;

import com.daksh.ibm.intenship.learningportal.model.Course;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CourseRepository extends ReactiveMongoRepository<Course, String> {
}