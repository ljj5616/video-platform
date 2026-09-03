package com.example.videoplatform.category.repository;

import com.example.videoplatform.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
