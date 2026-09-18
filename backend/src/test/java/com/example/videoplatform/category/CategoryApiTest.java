package com.example.videoplatform.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long",
        "spring.datasource.url=jdbc:h2:mem:category-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CategoryApiTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository repository;

    @Test
    void anonymousRequestReturnsFlatListSortedByDisplayOrderThenId() throws Exception {
        Category later = repository.save(category("나중", 10));
        Category first = repository.save(category("처음", 0));
        Category second = repository.save(category("두번째", 0));
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("""
                        [{"id":%d,"name":"처음"},{"id":%d,"name":"두번째"},{"id":%d,"name":"나중"}]
                        """.formatted(first.getId(), second.getId(), later.getId())))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[1].id").value(second.getId()))
                .andExpect(jsonPath("$[2].id").value(later.getId()))
                .andExpect(jsonPath("$[0].parent").doesNotExist())
                .andExpect(jsonPath("$[0].displayOrder").doesNotExist());
    }

    @Test
    void emptyListReturns200AndEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private Category category(String name, int order) {
        Category category = BeanUtils.instantiateClass(Category.class);
        ReflectionTestUtils.setField(category, "name", name);
        ReflectionTestUtils.setField(category, "displayOrder", order);
        return category;
    }
}
