package com.daksh.ibm.intenship.learningportal;

import com.daksh.ibm.intenship.learningportal.model.Category;
import com.daksh.ibm.intenship.learningportal.model.Course;
import com.daksh.ibm.intenship.learningportal.repository.CategoryRepository;
import com.daksh.ibm.intenship.learningportal.repository.CourseRepository;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.regions.Region;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux;

import java.util.Arrays;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
@EnableSwagger2WebFlux
public class LearningPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(LearningPortalApplication.class, args);
	}

	@Bean
	CommandLineRunner init(CategoryRepository categoryRepository,
						   CourseRepository courseRepository) {

		return args -> {
			Course course = new Course();
			course.setName("Python");
			course.setId("1");
			Category cat = new Category();
			cat.setName("Daksh");
			Flux<Category> categoryFlux = Flux.just(cat).flatMap(categoryRepository::save);

			categoryFlux
					.thenMany(categoryRepository.findAll())
					.subscribe(System.out::println);


			Flux<Course> courseFlux = Flux.just(course).flatMap(courseRepository::save);

			courseFlux
					.thenMany(courseRepository.findAll())
					.subscribe(System.out::println);
		};

	}

}