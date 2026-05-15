package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;

import java.util.Collection;

public interface PublicCategoryService {
    Collection<CategoryDto> findAllCategories(Integer from, Integer size);

    CategoryDto findCategoryById(Long id);
}
