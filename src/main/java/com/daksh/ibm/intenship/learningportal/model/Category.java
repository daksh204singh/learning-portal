package com.daksh.ibm.intenship.learningportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
@EqualsAndHashCode
@ToString
public class Category {

    @Id
    private String id;

    private String name;

    private List<String> courseList;

    public Category() {
        courseList = new ArrayList<>();
    }

    public Category(String id, String name, List<String> courseList) {
        this.id = id;
        this.name = name;
        this.courseList = courseList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCourseList() {
        return courseList;
    }

    public void setCourseList(List<String> courseList) {
        this.courseList = courseList;
    }
}
