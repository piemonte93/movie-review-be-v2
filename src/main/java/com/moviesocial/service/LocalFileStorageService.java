package com.moviesocial.service;

import com.moviesocial.config.AppProperties;
import com.moviesocial.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class LocalFileStorageService {

    private final Path fileStorageLocation;
    private final String fileStorageUrlPrefix = "/images/profile-images"; // 웹 접근 URL Prefix
    private final String profileImagesDir = "profile-images";

    @Autowired
    public LocalFileStorageService(AppProperties appProperties) {
        // Ensure the base upload directory exists
        Path baseUploadPath = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseUploadPath);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the base directory where the uploaded files will be stored.", ex);
        }

        // Set the specific location for profile images
        this.fileStorageLocation = baseUploadPath.resolve(profileImagesDir).normalize();

        // Ensure the profile images directory exists
        try {
            Files.createDirectories(this.fileStorageLocation);
            System.out.println("Profile image storage location created/verified: " + this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory for profile images.", ex);
        }
    }

    public String storeFile(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty.");
        }
        if (userId == null) {
             throw new IllegalArgumentException("User ID cannot be null for storing profile image.");
        }

        // 파일명 생성 (충돌 방지: 사용자 ID + 시간 + 원본 확장자)
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
             fileExtension = originalFileName.substring(lastDotIndex);
        } else {
            // 확장자가 없거나 잘못된 경우 기본 확장자 또는 에러 처리
            // 여기서는 예시로 .jpg를 사용하거나, content-type 기반으로 결정할 수도 있습니다.
             System.err.println("Warning: Could not determine file extension for: " + originalFileName + ". Using default or potentially incorrect extension.");
             // fileExtension = ".jpg"; // 또는 예외 발생
             // throw new FileStorageException("Invalid file name or missing extension: " + originalFileName);
        }

        // 기본적인 이미지 타입 검사 (선택 사항, 더 강력한 검사 필요할 수 있음)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileStorageException("Invalid file type. Only image files are allowed. Detected: " + contentType);
        }

        String fileName = userId + "_" + System.currentTimeMillis() + fileExtension;

        // 파일 저장
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
             System.out.println("Stored file: " + targetLocation);
        } catch (IOException ex) {
            System.err.println("Failed to store file " + fileName + " due to: " + ex.getMessage());
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }

        // 웹 접근 URL 반환
        return fileStorageUrlPrefix + "/" + fileName;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty() || !fileUrl.startsWith(fileStorageUrlPrefix)) {
            System.out.println("Skipping deletion for invalid or non-profile image URL: " + fileUrl);
            return;
        }

        // 기본 이미지 URL 등 삭제하면 안 되는 URL 확인 (isDefaultImage 같은 메서드 활용)
        if (isDefaultImage(fileUrl)) { // 예시: 기본 이미지는 삭제하지 않음
             System.out.println("Skipping deletion for default image URL: " + fileUrl);
             return;
        }

        try {
            String fileName = fileUrl.substring(fileStorageUrlPrefix.length() + 1);
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("Deleted file: " + filePath);
            } else {
                 System.out.println("File not found for deletion: " + filePath);
            }
        } catch (NoSuchFileException ex) {
             System.err.println("File not found during deletion attempt: " + fileUrl);
        } catch (DirectoryNotEmptyException ex) {
             System.err.println("Directory not empty error during deletion: " + fileUrl);
        } catch (IOException ex) {
            System.err.println("Could not delete file: " + fileUrl + " due to " + ex.getMessage());
            // 로깅 강화, 실패 시 알림 등 고려
        }
    }

    // 실제 기본 이미지 URL 확인 로직 구현 필요
    private boolean isDefaultImage(String url) {
        // 예: application.properties 등에서 기본 이미지 URL 목록을 관리하고 비교
        // return appProperties.getDefaultProfileImageUrls().contains(url);
        return false; // 임시로 false 반환, 실제 로직 구현 필요
    }
} 