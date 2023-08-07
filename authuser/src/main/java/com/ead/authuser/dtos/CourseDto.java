package com.ead.authuser.dtos;

import com.ead.authuser.enums.CourseStatus;
import com.ead.authuser.enums.CourseLevel;
import lombok.Data;

import java.util.UUID;

@Data
public class CourseDto {

    private UUID courseId;
    private String name;
    private String description;
    private String imageUrl;
    private CourseStatus courseStatus;
    private UUID userInstructor;
    private CourseLevel courseLevel;
}
