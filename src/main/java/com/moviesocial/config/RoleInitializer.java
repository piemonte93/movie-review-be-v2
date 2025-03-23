package com.moviesocial.config;

import com.moviesocial.model.ERole;
import com.moviesocial.model.Role;
import com.moviesocial.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // 역할 테이블이 비어있는지 확인
        if (roleRepository.count() == 0) {
            // 기본 역할 생성
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role modRole = new Role();
            modRole.setName(ERole.ROLE_MODERATOR);
            roleRepository.save(modRole);

            System.out.println("기본 역할이 성공적으로 초기화되었습니다.");
        }
    }
}