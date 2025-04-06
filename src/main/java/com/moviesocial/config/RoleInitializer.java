package com.moviesocial.config;

import com.moviesocial.model.ERole;
import com.moviesocial.model.Role;
import com.moviesocial.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        try {
            // USER 역할이 없으면 기본 역할들 생성
            if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
                System.out.println("기본 역할 초기화 시작");
                
                Role userRole = new Role(ERole.ROLE_USER);
                roleRepository.save(userRole);
                System.out.println("USER 역할 생성됨");

                Role adminRole = new Role(ERole.ROLE_ADMIN);
                roleRepository.save(adminRole);
                System.out.println("ADMIN 역할 생성됨");

                Role modRole = new Role(ERole.ROLE_MODERATOR);
                roleRepository.save(modRole);
                System.out.println("MODERATOR 역할 생성됨");
                
                System.out.println("기본 역할 초기화 완료");
            }
        } catch (Exception e) {
            System.err.println("역할 초기화 중 오류 발생: " + e.getMessage());
        }
    }
}