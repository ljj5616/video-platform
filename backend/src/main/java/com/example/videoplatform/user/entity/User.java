package com.example.videoplatform.user.entity;

import com.example.videoplatform.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 1000)
    private String profileImageUrl;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime deletedAt;

    public static User create(String email, String passwordHash, String nickname, String name, String phone) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.name = name;
        user.phone = phone;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = withdrawnAt;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
