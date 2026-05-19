package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        log.debug("Создание записи о новой подборке событии {}", request.getTitle());

        if (request.getPinned() == null) {
            request.setPinned(false);
        }

        Compilation compilation = CompilationMapper.mapToCompilation(request, findEvents(request.getEvents()));

        compilation = compilationRepository.save(compilation);

        log.info("Сохранение данных о подборке {}", request.getTitle());
        return CompilationMapper.mapToCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.debug("Удаление подборки c ID {}", compId);
        Compilation compilation = findCompilation(compId);
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.debug("Обновление подборки c ID {}", compId);
        Compilation existingCompilation = findCompilation(compId);

        Set<Event> events = null;
        if (request.getEvents() != null) {
            events = request.getEvents().isEmpty() ? new HashSet<>() :
                    new HashSet<>(eventRepository.findAllById(request.getEvents()));
        }

        CompilationMapper.updateFields(existingCompilation, request, events);

        existingCompilation = compilationRepository.save(existingCompilation);

        log.info("Подборка с ID {} успешно обновлена", compId);
        return CompilationMapper.mapToCompilationDto(existingCompilation);
    }

    @Override
    public Collection<CompilationDto> getCompilations(Integer from, Integer size) {
        log.debug("Получение списка подборок: from={}, size={}", from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);

        return compilationRepository.findAll(pageRequest).getContent().stream()
                .map(CompilationMapper::mapToCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<CompilationDto> getCompilationsByPinned(Boolean pinned, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest);
        } else {
            compilations = compilationRepository.findAll(pageRequest).toList();
        }

        return CompilationMapper.mapToListDto(compilations);
    }

    @Override
    public CompilationDto findCompilationById(Long id) {
        log.debug("Получение подборки c ID {}", id);
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка с ID " + id + " не найдена"));

        return CompilationMapper.mapToCompilationDto(compilation);
    }

    public Compilation findCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с ID " + compId + " не найдена"));
    }

    private Set<Event> findEvents(Set<Long> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptySet();
        }

        return new HashSet<>(eventRepository.findAllById(events));
    }
}
