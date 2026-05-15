package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PublicEventService {
    Collection<EventShortDto> findPublicEvents(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort, Integer from, Integer size,
                                                  HttpServletRequest request);

    EventFullDto findPublicEventById(Long id, HttpServletRequest request);
}
