package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;

public interface AdminCategoryService {
    CategoryDto createCategory(NewCategoryDto request);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto category);
}
