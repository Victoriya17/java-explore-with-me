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
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.enums.ParticipationRequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

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

        statsClient.save(new EndpointHitDto(null, "ewm-main-service", request.getRequestURI(),
                request.getRemoteAddr(), LocalDateTime.now()));

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        return enrichAndSortEvents(events, sort);
    }

    @Override
    @Transactional
    public EventFullDto findPublicEventById(Long id, HttpServletRequest request) {
        log.info("Поиск публичного события по ID: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + id + " не найдено"));

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с ID " + id + " не опубликовано");
        }

        hydrateConfirmedRequests(event);
        sendHitToStats(request);

        long views = getUniqueViews(request.getRequestURI());
        event.setViews(views);

        EventFullDto result = EventMapper.mapToFullEventDto(eventRepository.save(event));
        log.info("Завершено получение полного DTO для события ID {}, установлено просмотров: {}", id, views);
        return result;
    }

    private void sendHitToStats(HttpServletRequest request) {
        try {
            statsClient.save(new EndpointHitDto(null, "ewm-main-service", request.getRequestURI(),
                    request.getRemoteAddr(), LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Не удалось сохранить хит в статистику", e);
        }
    }

    private long getUniqueViews(String requestUri) {
        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now().plusYears(10);

        try {
            ResponseEntity<Object> response = statsClient.get(start, end, List.of(requestUri), true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {});

                if (stats != null && !stats.isEmpty()) {
                    return stats.get(0).getHits();
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении уникальной статистики", e);
        }
        return 1L;
    }

    private List<EventShortDto> enrichAndSortEvents(List<Event> events, String sort) {
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<String, Long> viewsMap = getViewsMapFromStats(uris);

        for (Event event : events) {
            hydrateConfirmedRequests(event);
            String eventUri = "/events/" + event.getId();
            event.setViews(viewsMap.getOrDefault(eventUri, 0L));
        }

        List<EventShortDto> resultDtos = EventMapper.mapToListShortEventDto(events);

        if ("VIEWS".equalsIgnoreCase(sort)) {
            resultDtos.sort((o1, o2) -> o2.getViews().compareTo(o1.getViews()));
        }

        return resultDtos;
    }

    private void hydrateConfirmedRequests(Event event) {
        long confirmedRequests = requestRepository.countByEventIdAndStatus(
                event.getId(), ParticipationRequestStatus.CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);
        log.debug("Для события ID {} установлено подтверждённых запросов: {}", event.getId(), confirmedRequests);
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

    private Map<String, Long> getViewsMapFromStats(List<String> uris) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusYears(10);

            log.debug("Запрос карты просмотров для URI: {} с {} по {}", uris, start, end);

            ResponseEntity<Object> response = statsClient.get(start, end, uris, false);

            log.debug("Ответ от сервиса статистики при получении карты просмотров: статус {}, тело {}",
                    response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {});

                Map<String, Long> viewsMap = stats.stream()
                        .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits, (a, b) -> a));

                log.debug("Получена карта просмотров: {}", viewsMap);
                return viewsMap;
            }
        } catch (Exception e) {
            log.error("Ошибка при получении карты просмотров из сервиса статистики для URI: {}", uris, e);
        }
        log.warn("Не удалось получить карту просмотров, возвращаем пустую карту");
        return Collections.emptyMap();
    }
}