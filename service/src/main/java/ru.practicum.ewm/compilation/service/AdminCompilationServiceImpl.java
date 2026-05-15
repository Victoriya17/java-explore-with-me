package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.BadRequestException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCompilationServiceImpl implements AdminCompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        log.debug("Создание записи о новой подборке событии {}", request.getTitle());
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Имя подборки не может быть пустым.");
        }

        if (request.getPinned() == null) {
            request.setPinned(false);
        }

        Compilation compilation = CompilationMapper.mapToCompilation(request, findEvents(request.getEvents()));

        try {
            compilation = compilationRepository.save(compilation);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Подборка с таким заголовком уже существует");
        }

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

        try {
            existingCompilation = compilationRepository.save(existingCompilation);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Подборка с таким именем уже существует");
        }

        log.info("Подборка с ID {} успешно обновлена", compId);
        return CompilationMapper.mapToCompilationDto(existingCompilation);
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
