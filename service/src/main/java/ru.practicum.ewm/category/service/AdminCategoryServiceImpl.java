package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto request) {
        log.debug("Создание записи о новой категории {}", request.getName());
        Category category = CategoryMapper.mapToCategory(request);

        try {
            category = categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с таким именем уже существует");
        }

        log.info("Сохранение данных о категории {}", request.getName());
        return CategoryMapper.mapToCategoryDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.debug("Удаление категории c ID {}", catId);
        Category category = findCategory(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Категория не пуста: с ней связаны события");
        }

        log.debug("Удаление категории {}", category.getName());
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto category) {
        log.debug("Обновление категории c ID {}", catId);
        Category existingCategory = findCategory(catId);

        CategoryMapper.updateFields(existingCategory, category);

        try {
            existingCategory = categoryRepository.saveAndFlush(existingCategory);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с таким именем уже существует");
        }

        log.info("Категория с ID {} успешно обновлена", catId);
        return CategoryMapper.mapToCategoryDto(existingCategory);
    }

    public Category findCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));
    }
}
