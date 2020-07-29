package com.daksh.ibm.intenship.learningportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.joda.time.DateTime;

import java.util.Date;

@ToString
@EqualsAndHashCode
public class Lecture {

    @Id
    private String id;

    private String duration;

    @JsonProperty
    private Date createdDate;

    private String name;

    private String description;

    private String contentKey;

    private String contentUrl;

    public Lecture() {
        createdDate = DateTime.now().toDate();
    }

    public Lecture(String id, String duration, Date createdDate, String name, String description, String contentKey, String contentUrl) {
        this.id = id;
        this.duration = duration;
        this.createdDate = createdDate;
        this.name = name;
        this.description = description;
        this.contentKey = contentKey;
        this.contentUrl = contentUrl;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }
}
