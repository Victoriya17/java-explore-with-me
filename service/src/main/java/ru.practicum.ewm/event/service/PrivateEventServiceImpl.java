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
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.enums.ParticipationRequestStatus;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional(readOnly = true)
    public Collection<EventShortDto> getEvents(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        log.info("Получено {} событий пользователя с ID {}", events.size(), userId);

        return EventMapper.mapToListShortEventDto(events);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        log.debug("Создание нового события текущим пользователем {}", request.getTitle());
        checkEventDate(request.getEventDate());

        Event event = EventMapper.mapToEvent(request, findCategoryById(request.getCategory()), findUserById(userId));

        event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));

        try {
            event = eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Событие с таким заголовком уже существует");
        }
        log.info("Сохранение данных о событии {}", request.getTitle());
        return EventMapper.mapToFullEventDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto findEvent(Long userId, Long eventId) {
        Event events = findByIdAndInitiatorId(eventId, userId);

        log.info("Событие {} пользователя {} найдено", events.getTitle(), events.getInitiator().getName());
        return EventMapper.mapToFullEventDto(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event findEvent = findByIdAndInitiatorId(eventId, userId);

        if (findEvent.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Изменить можно только отмененные события или события в состоянии ожидания " +
                    "модерации");
        }

        if (request.hasEventDate()) {
            checkEventDate(request.getEventDate());
        }

        if (request.hasLocation()) {
            Location newLocation = new Location(request.getLocation().getLat(), request.getLocation().getLon());
            findEvent.setLocation(newLocation);
        }

        Category category = null;
        if (request.hasCategory() && !findEvent.getCategory().getId().equals(request.getCategory())) {
            category = findCategoryById(request.getCategory());
        }

        Event updatedEvent = EventMapper.updateUserFields(findEvent, request, category);

        try {
            updatedEvent = eventRepository.save(updatedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Событие уже существует");
        }

        log.info("Пользователь с ID {} обновляет событие {}", userId, updatedEvent.getTitle());
        return EventMapper.mapToFullEventDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        Event event = findByIdAndInitiatorId(eventId, userId);

        log.info("Получение данных о всех запросах на участие в событии {} пользователях c ID {}", event.getTitle(),
                userId);
        return RequestMapper.mapToListDto(requestRepository.findAllByEventId(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatusEvent(Long userId, Long eventId,
                                                            EventRequestStatusUpdateRequest request) {
        log.debug("Обновление статуса заявок для события ID {} пользователем ID {}", eventId, userId);

        Event event = findByIdAndInitiatorId(eventId, userId);

        List<Request> requests = requestRepository.findAllById(request.getRequestIds());

        validateRequestStatusUpdate(event, requests);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (request.getStatus().name().equals(ParticipationRequestStatus.CONFIRMED.name())) {
            for (Request req : requests) {
                if (event.getParticipantLimit() == 0 || event.getConfirmedRequests() < event.getParticipantLimit()) {
                    req.setStatus(ParticipationRequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    result.getConfirmedRequests().add(RequestMapper.mapToRequestDto(req));
                } else {
                    req.setStatus(ParticipationRequestStatus.REJECTED);
                    result.getRejectedRequests().add(RequestMapper.mapToRequestDto(req));
                }
            }
        } else {
            for (Request req : requests) {
                req.setStatus(ParticipationRequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.mapToRequestDto(req));
            }
        }

        requestRepository.saveAll(requests);
        eventRepository.save(event);

        log.info("Обновлено заявок: подтверждено {}, отклонено {}",
                result.getConfirmedRequests().size(), result.getRejectedRequests().size());

        return result;
    }

    private void validateRequestStatusUpdate(Event event, List<Request> requests) {
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Для данного события подтверждение заявок не требуется");
        }

        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит заявок на данное событие исчерпан");
        }

        for (Request req : requests) {
            if (!req.getStatus().equals(ParticipationRequestStatus.PENDING)) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }
        }
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с ID " + catId + " не найдена")));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID " + userId + " не найден")));
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата и время, на которые намечено событие, не могут быть раньше, чем через" +
                    " 2 часа от текущего момента: " + eventDate);
        }
    }

    private Event findByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID " + eventId + " пользователя " +
                        "c ID " + userId + " не найдено")));
    }
}
