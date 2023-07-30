package com.ead.course.dtos;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ModuleDto {

    @NotBlank
    private String name;

    @NotBlank
    private String description;
}
