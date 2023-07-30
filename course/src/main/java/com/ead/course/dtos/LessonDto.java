package com.ead.course.dtos;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LessonDto {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String videoUrl;
}
