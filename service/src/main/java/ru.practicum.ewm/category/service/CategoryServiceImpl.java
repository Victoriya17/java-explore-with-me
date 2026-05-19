package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto request) {
        log.debug("Создание записи о новой категории {}", request.getName());
        Category category = CategoryMapper.mapToCategory(request);

        category = categoryRepository.save(category);

        log.info("Сохранение данных о категории {}", request.getName());
        return CategoryMapper.mapToCategoryDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.debug("Удаление категории c ID {}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));

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
        Category existingCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));

        CategoryMapper.updateFields(existingCategory, category);

        existingCategory = categoryRepository.save(existingCategory);

        log.info("Категория с ID {} успешно обновлена", catId);
        return CategoryMapper.mapToCategoryDto(existingCategory);
    }

    @Override
    public Collection<CategoryDto> findAllCategories(Integer from, Integer size) {
        log.debug("Получение списка категорий: from={}, size={}", from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageRequest).getContent().stream()
                .map(CategoryMapper::mapToCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto findCategoryById(Long id) {
        log.debug("Получение категории c ID {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + id + " не найдена"));

        return CategoryMapper.mapToCategoryDto(category);
    }

    @Override
    public Category findCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));
    }
}
