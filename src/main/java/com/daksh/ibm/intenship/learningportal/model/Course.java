package com.daksh.ibm.intenship.learningportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Document
@ToString
@EqualsAndHashCode
public class Course {
    int count = 0;

    @Id
    private String id;

    private String name;

    private String description;

    private Double price;

    @JsonProperty
    private Date createdDate;

    @JsonProperty
    private Map<String, Lecture> lectures;

    @JsonProperty
    private List<String> categories;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Course() {
        categories = new ArrayList<>();
        lectures = new HashMap<>();
        createdDate = DateTime.now().toDate();
    }

    public Map<String, Lecture> getLectures() {
        return lectures;
    }

    public Lecture getLecture(String lectureId) {
        return lectures.get(lectureId);
    }

    public Lecture putLecture(String lectureId, Lecture lecture) {
        lecture.setId(lectureId);
        return lectures.put(lectureId, lecture);
    }

    public Lecture addLecture(Lecture lecture) {
        lecture.setId(Integer.toString(count++));
        return lectures.put(lecture.getId(), lecture);
    }

    public void setLectures(Map<String, Lecture> lectures) {
        this.lectures = lectures;
    }

    public Course(String id, String name, String description, Double price,
                  Date createdDate, Map<String, Lecture> lectures, List<String> categories) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdDate = createdDate;
        this.lectures = lectures;
        this.categories = categories;
    }
}
