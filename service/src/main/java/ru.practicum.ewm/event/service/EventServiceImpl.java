package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.enums.AdminStateAction;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;
    private final CategoryService categoryService;

    @Override
    public Collection<EventFullDto> findAdminEvents(AdminEventParams params) {
        log.debug("Поиск событий администратором: users={}, states={}, categories={}",
                params.getUsers(), params.getStates(), params.getCategories());

        List<State> eventStates = null;
        if (params.getStates() != null && !params.getStates().isEmpty()) {
            eventStates = params.getStates().stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by("id").ascending()
        );

        List<Event> events = eventRepository.findAdminEvents(
                params.getUsers(),
                eventStates,
                params.getCategories(),
                params.getRangeStart(),
                params.getRangeEnd(),
                pageable
        );

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Найдено {} событий для администратора", events.size());

        return EventMapper.mapToListFullEventDto(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event findEvent = findEventById(eventId);

        validateAdminUpdate(findEvent, request);

        Category category = null;
        if (request.hasCategory()) {
            category = categoryService.findCategory(request.getCategory());
        }

        Event updatedEvent = EventMapper.updateAdminFields(findEvent, request, category);

        updatedEvent = eventRepository.save(updatedEvent);

        log.info("Администратор обновляет событие \"{}\"", updatedEvent.getTitle());
        return EventMapper.mapToFullEventDto(updatedEvent);
    }

    @Override
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

        Event event = EventMapper.mapToEvent(request, categoryService.findCategory(request.getCategory()),
                findUserById(userId));

        event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));

        event = eventRepository.save(event);

        log.info("Сохранение данных о событии {}", request.getTitle());
        return EventMapper.mapToFullEventDto(event);
    }

    @Override
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
            category = categoryService.findCategory(request.getCategory());
        }

        Event updatedEvent = EventMapper.updateUserFields(findEvent, request, category);

        updatedEvent = eventRepository.save(updatedEvent);

        log.info("Пользователь с ID {} обновляет событие {}", userId, updatedEvent.getTitle());
        return EventMapper.mapToFullEventDto(updatedEvent);
    }

    @Override
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

    @Override
    public Collection<EventShortDto> findPublicEvents(String text, List<Long> categories, Boolean paid,
                                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                      Boolean onlyAvailable, String sort, Integer from, Integer size,
                                                      HttpServletRequest request) {

        validateSearchRange(rangeStart, rangeEnd);
        Pageable pageable = getPageable(sort, from, size);
        LocalDateTime start = (rangeStart == null && rangeEnd == null) ? LocalDateTime.now() : rangeStart;
        String textPattern = (text != null && !text.isBlank()) ? "%" + text.toLowerCase() + "%" : null;

        List<Event> events = eventRepository.findPublicEventsWithFilters(
                textPattern, categories, paid, start, rangeEnd,
                Boolean.TRUE.equals(onlyAvailable), pageable
        );

        statsClient.save(request);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        return setEventStatsAndSort(events, sort);
    }

    @Override
    public EventFullDto findPublicEventById(Long id, HttpServletRequest request) {
        log.info("Поиск публичного события по ID: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + id + " не найдено"));

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с ID " + id + " не опубликовано");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(
                event.getId(), ParticipationRequestStatus.CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);
        log.debug("Для события ID {} установлено подтверждённых запросов: {}", event.getId(), confirmedRequests);

        try {
            statsClient.save(request);
        } catch (Exception e) {
            log.error("Не удалось сохранить хит в статистику", e);
        }

        long views = getUniqueViews(request.getRequestURI());
        event.setViews(views);

        EventFullDto result = EventMapper.mapToFullEventDto(eventRepository.save(event));
        log.info("Завершено получение полного DTO для события ID {}, установлено просмотров: {}", id, views);
        return result;
    }

    private long getUniqueViews(String requestUri) {
        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now().plusYears(10);

        try {
            ResponseEntity<Object> response = statsClient.get(start, end, List.of(requestUri), true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {
                        });

                if (stats != null && !stats.isEmpty()) {
                    return stats.get(0).getHits();
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении уникальной статистики", e);
        }
        return 1L;
    }

    private List<EventShortDto> setEventStatsAndSort(List<Event> events, String sort) {
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toList());

        Map<String, Long> viewsMap = getViewsMapFromStats(uris, events);

        Map<Long, Long> confirmedRequestsMap = requestRepository.findAllByEventIdInAndStatus(eventIds,
                        ParticipationRequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.groupingBy(request -> request.getEvent().getId(), Collectors.counting()));

        for (Event event : events) {
            String eventUri = "/events/" + event.getId();
            event.setViews(viewsMap.getOrDefault(eventUri, 0L));

            long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            event.setConfirmedRequests(confirmedRequests);
        }

        List<EventShortDto> resultDto = EventMapper.mapToListShortEventDto(events);

        if ("VIEWS".equalsIgnoreCase(sort)) {
            resultDto.sort((o1, o2) -> o2.getViews().compareTo(o1.getViews()));
        }

        return resultDto;
    }

    private void validateSearchRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("Конец не может быть раньше начала");
        }
    }

    private Pageable getPageable(String sort, Integer from, Integer size) {
        Sort sortOrder = switch (sort != null ? sort.toUpperCase() : "NONE") {
            case "EVENT_DATE" -> Sort.by("eventDate").ascending();
            case "VIEWS" -> Sort.by("views").descending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(from / size, size, sortOrder);
        log.debug("Создан Pageable: page={}, size={}, sort={}", from / size, size, sort);
        return pageable;
    }

    private Map<String, Long> getViewsMapFromStats(List<String> uris, List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = events.get(0).getPublishedOn();
        LocalDateTime end = LocalDateTime.now();

        try {
            log.debug("Запрос карты просмотров для URI: {} с {} по {}", uris, start, end);

            ResponseEntity<Object> response = statsClient.get(start, end, uris, false);

            log.debug("Ответ от сервиса статистики при получении карты просмотров: статус {}, тело {}",
                    response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {
                        });

                return stats.stream()
                        .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits, (a, b) -> a));
            }
        } catch (Exception e) {
            log.error("Ошибка при получении карты просмотров из сервиса статистики для URI: {}", uris, e);
        }
        log.warn("Не удалось получить карту просмотров, возвращаем пустую карту");
        return Collections.emptyMap();
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

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID " + eventId + " не найдено")));
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
