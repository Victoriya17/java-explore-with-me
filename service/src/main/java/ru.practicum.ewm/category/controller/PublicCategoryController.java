package ru.practicum.ewm.category.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.service.PublicCategoryService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/categories")
public class PublicCategoryController {
    private final PublicCategoryService categoryService;

    @GetMapping
    public Collection<CategoryDto> findAllCategories(
            @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
            @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return categoryService.findAllCategories(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryDto findCategoryById(@PathVariable("catId") @Positive Long id) {
        return categoryService.findCategoryById(id);
    }
}
