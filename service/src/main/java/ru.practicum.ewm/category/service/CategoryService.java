package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;

import java.util.Collection;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto request);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto category);

    Collection<CategoryDto> findAllCategories(Integer from, Integer size);

    CategoryDto findCategoryById(Long id);

    Category findCategory(Long catId);
}
