package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventShortDto> findAllEvents(@PathVariable("userId") @Positive Long userId,
                                                   @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                                   @Positive @RequestParam(defaultValue = "10") Integer size) {
        return eventService.getEvents(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable("userId") @Positive Long userId,
                               @Valid @RequestBody NewEventDto request) {
        return eventService.createEvent(userId, request);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findUserEvent(@PathVariable("userId") @Positive Long userId,
                                      @PathVariable("eventId") @Positive Long eventId) {
        return eventService.findEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable("userId") @Positive Long userId,
                                    @PathVariable("eventId") @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequest request) {
        return eventService.updateEvent(userId, eventId, request);
    }

    @GetMapping("/{eventId}/requests")
    public Collection<ParticipationRequestDto> findAllUserRequests(@PathVariable("userId") @Positive Long userId,
                                                               @PathVariable("eventId") @Positive Long eventId) {
        return eventService.getRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatusEvent(@PathVariable("userId") @Positive Long userId,
                                                            @PathVariable("eventId") @Positive Long eventId,
                                                            @RequestBody EventRequestStatusUpdateRequest request) {
        return eventService.updateStatusEvent(userId, eventId, request);
    }
}
