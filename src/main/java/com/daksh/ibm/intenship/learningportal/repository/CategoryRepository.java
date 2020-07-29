package com.daksh.ibm.intenship.learningportal.repository;

import com.daksh.ibm.intenship.learningportal.model.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {

}
