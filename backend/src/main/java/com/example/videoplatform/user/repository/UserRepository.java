package com.example.videoplatform.user.repository;

import com.example.videoplatform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByNameAndPhoneAndStatus(String name, String phone, com.example.videoplatform.user.entity.UserStatus status);
}
