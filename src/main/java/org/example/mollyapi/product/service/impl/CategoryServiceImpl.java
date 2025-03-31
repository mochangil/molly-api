package org.example.mollyapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.CategoryError;
import org.example.mollyapi.product.entity.Category;
import org.example.mollyapi.product.repository.CategoryRepository;
import org.example.mollyapi.product.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.example.mollyapi.common.exception.error.impl.CategoryError.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private ApplicationContext applicationContext;

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> findByName(String name) {
        return findByCategoryName(name);
    }

    @Override
    public List<String> getCategoryPath(Category category) {
        List<String> path = new ArrayList<>();

        // 루프를 통해 카테고리의 족보를 찾음
        while (category != null) {
            path.add(category.getCategoryName());  // 현재 카테고리 이름을 추가
            category = category.getParent();  // 부모 카테고리로 이동
        }

        Collections.reverse(path);
        return path;
    }

    // @Cacheable(value = "categoryPaths", key = "#id")
    @Override
    public List<String> getCategoryPath(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(NOT_EXIST_CATEGORY));

        return getCategoryPath(category);
    }

    @Override
    public Category getCategory(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Categories cannot be empty");
        }

        String targetCategoryName = categories.get(categories.size() - 1);
        List<Category> categoriesList = findByName(targetCategoryName);

        for (Category category : categoriesList) {
            List<String> categoryPath = getCategoryPath(category);
            if (categories.equals(categoryPath)) {
                return category;
            }
        }

        throw new IllegalArgumentException("Category not found");
    }

    @Override
    public List<Category> findEndWith(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        String end = categories.get(categories.size() - 1);
        CategoryServiceImpl self = applicationContext.getBean(CategoryServiceImpl.class);
        List<Category> categoryList = self.findByCategoryName(end);

        return categoryList.stream().filter((category) -> isEndWith(category, categories)).toList();
    }

    // @Cacheable(value = "categories", key = "#categoryName")
    public List<Category> findByCategoryName(String categoryName) {
        // log.info("findByCategoryName at DB: {}", categoryName);
        return categoryRepository.findByCategoryName(categoryName);
    }

    private Boolean isEndWith(Category category, List<String> categories) {
        Category c = category;
        List<String> newCategories = new ArrayList<>(categories);
        Collections.reverse(newCategories);

        for (String categoryName : newCategories) {
            if (categoryName.equals(c.getCategoryName())) {
                c = c.getParent();
            } else {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public List<Category> getLeafCategories(Category category) {
        List<Category> categories = new ArrayList<>();
        List<Category> children = category.getChildren();

        // 자식이 없는 경우 자신을 리프 노드로 간주하고 추가
        if (children.isEmpty()) {
            categories.add(category);
            return categories;
        }

        // 자식이 있다면 재귀적으로 탐색
        for (Category child : children) {
            categories.addAll(getLeafCategories(child));
        }

        return categories;
    }

    @Override
    public List<Category> getAllLeafCategories(List<Category> categoryList) {
        List<Category> leafCategoryList = new ArrayList<>();

        for (Category category : categoryList) {
            leafCategoryList.addAll(getLeafCategories(category));
        }

        return leafCategoryList;
    }

    @Override
    public List<Category> findEndWith(String categories) {
        List<String> categoryPath = categories == null ? null : Arrays.asList(categories.split(","));

        return findEndWith(categoryPath);
    }
}
