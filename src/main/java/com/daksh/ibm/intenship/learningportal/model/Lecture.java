package com.daksh.ibm.intenship.learningportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @JsonProperty
    private Map<String, String> contentUrls;

    public Lecture() {
        createdDate = DateTime.now().toDate();
        contentUrls = new HashMap<>();
    }

    public Lecture(String id, String duration, Date createdDate, String name, String description, Map<String, String> contentUrls) {
        this.id = id;
        this.duration = duration;
        this.createdDate = createdDate;
        this.name = name;
        this.description = description;
        this.contentUrls = contentUrls;
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

    public void putContent(String fileKey, String url) {
        this.contentUrls.put(fileKey, url);
    }

    public Map<String, String> getContentUrls() {
        return contentUrls;
    }

    public void setContentUrls(Map<String, String> contentUrls) {
        this.contentUrls = contentUrls;
    }

}
