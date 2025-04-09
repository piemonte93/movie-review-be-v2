package com.moviesocial.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequestDto {

    // Add validation constraints as needed
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @Size(max = 100, message = "Bio cannot exceed 100 characters")
    private String bio;

    // profileImageUrl is handled separately via MultipartFile
} 