package ru.practicum.ewm.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.PublicCompilationService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/compilations")
public class PublicCompilationController {
    private final PublicCompilationService compilationService;

    @GetMapping
    public Collection<CompilationDto> findAllCompilations(@RequestParam(required = false) Boolean pinned,
                                      @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                      @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        if (pinned == null) {
            return compilationService.getCompilations(from, size);
        }
        return compilationService.getCompilationsByPinned(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto findCompilationById(@PathVariable("compId") @Positive Long id) {
        return compilationService.findCompilationById(id);
    }
}
