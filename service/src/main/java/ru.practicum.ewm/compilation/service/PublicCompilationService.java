package ru.practicum.ewm.compilation.service;

import ru.practicum.ewm.compilation.dto.CompilationDto;

import java.util.Collection;

public interface PublicCompilationService {
    Collection<CompilationDto> getCompilations(Integer from, Integer size);

    Collection<CompilationDto> getCompilationsByPinned(Boolean pinned, Integer from, Integer size);

    CompilationDto findCompilationById(Long id);
}
