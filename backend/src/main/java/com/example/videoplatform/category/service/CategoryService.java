package com.example.videoplatform.category.service;

import com.example.videoplatform.category.dto.CategoryResponse;
import com.example.videoplatform.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("displayOrder", "id")).stream()
                .map(CategoryResponse::from).toList();
    }
}
