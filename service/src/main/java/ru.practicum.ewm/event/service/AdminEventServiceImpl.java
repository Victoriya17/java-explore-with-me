package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.enums.AdminStateAction;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Collection<EventFullDto> findAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                                    LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from,
                                                    Integer size) {
        log.debug("Поиск событий администратором: users={}, states={}, categories={}", users, states, categories);

        List<State> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findAdminEvents(users, eventStates, categories, rangeStart, rangeEnd,
                pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Найдено {} событий для администратора", events.size());

        return EventMapper.mapToListFullEventDto(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        Event findEvent = findEventById(eventId);

        validateAdminUpdate(findEvent, request);

        Category category = null;
        if (request.hasCategory()) {
            category = findCategoryById(request.getCategory());
        }

        Event updatedEvent = EventMapper.updateAdminFields(findEvent, request, category);

        try {
            updatedEvent = eventRepository.save(updatedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Событие уже существует");
        }

        log.info("Администратор обновляет событие \"{}\"", updatedEvent.getTitle());
        return EventMapper.mapToFullEventDto(updatedEvent);
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID " + eventId + " не найдено")));
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с ID " + catId + " не найдена")));
    }

    private void validateAdminUpdate(Event event, UpdateEventAdminRequest request) {
        if (request.hasEventDate() && request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата начала события должна быть не ранее чем за час от даты публикации.");
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals(AdminStateAction.PUBLISH_EVENT)) {
                if (!event.getState().equals(State.PENDING)) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания");
                }
            }

            if (request.getStateAction().equals(AdminStateAction.REJECT_EVENT)) {
                if (event.getState().equals(State.PUBLISHED)) {
                    throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
                }
            }
        }
    }
}
