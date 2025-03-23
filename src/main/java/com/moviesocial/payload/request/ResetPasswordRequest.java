package com.moviesocial.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ResetPasswordRequest {
    private String token;
    
    @JsonProperty("newPassword")
    private String newPassword;
    
    // 디버깅을 위한 필드 접근 메소드 추가
    public String getDebugInfo() {
        return "Token: " + token + ", NewPassword exists: " + (newPassword != null);
    }
}