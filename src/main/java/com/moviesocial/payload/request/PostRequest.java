package com.moviesocial.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    
    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;
    
    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    @Size(min = 2, max = 5000, message = "내용은 2자 이상 5000자 이하로 입력해주세요.")
    private String content;
} 