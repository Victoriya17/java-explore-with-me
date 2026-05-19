package ru.practicum.ewm.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventMapper {
    public static EventFullDto mapToFullEventDto(Event event) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.mapToCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(UserMapper.mapToShortUserDto(event.getInitiator()));
        dto.setLocation(LocationMapper.mapToLocationDto(event.getLocation()));
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventShortDto mapToShortEventDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.mapToCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(UserMapper.mapToShortUserDto(event.getInitiator()));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static Event updateUserFields(Event event, UpdateEventUserRequest request, Category category) {
        if (request.hasAnnotation()) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.hasCategory()) {
            event.setCategory(category);
        }

        if (request.hasDescription()) {
            event.setDescription(request.getDescription());
        }

        if (request.hasEventDate()) {
            event.setEventDate(request.getEventDate());
        }

        if (request.hasLocation()) {
            event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));
        }

        if (request.hasPaid()) {
            event.setPaid(request.getPaid());
        }

        if (request.hasParticipantLimit()) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.hasRequestModeration()) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.hasTitle()) {
            event.setTitle(request.getTitle());
        }

        updateUserEventState(event, request);

        return event;
    }

    private static void updateUserEventState(Event event, UpdateEventUserRequest request) {
        if (request.hasStateAction()) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(State.PENDING);
                case CANCEL_REVIEW -> event.setState(State.CANCELED);
            }
        }
    }

    public static Event updateAdminFields(Event event, UpdateEventAdminRequest request, Category category) {
        if (request.hasAnnotation()) {
            event.setAnnotation(request.getAnnotation());
        }

        if (category != null) {
            event.setCategory(category);
        }

        if (request.hasDescription()) {
            event.setDescription(request.getDescription());
        }

        if (request.hasEventDate()) {
            event.setEventDate(request.getEventDate());
        }

        if (request.hasLocation()) {
            event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));
        }

        if (request.hasPaid()) {
            event.setPaid(request.getPaid());
        }

        if (request.hasParticipantLimit()) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.hasRequestModeration()) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.hasTitle()) {
            event.setTitle(request.getTitle());
        }

        updateAdminEventState(event, request);

        return event;
    }

    private static void updateAdminEventState(Event event, UpdateEventAdminRequest request) {
        if (request.hasStateAction()) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT -> event.setState(State.PUBLISHED);
                case REJECT_EVENT -> event.setState(State.CANCELED);
            }
        }
    }

    public static Event mapToEvent(NewEventDto dto, Category category, User initiator) {
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setConfirmedRequests(0L);
        event.setCreatedOn(LocalDateTime.now());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setInitiator(initiator);
        event.setLocation(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()));

        if (dto.hasPaid()) {
            event.setPaid(dto.getPaid());
        } else {
            event.setPaid(Boolean.FALSE);
        }

        if (dto.hasParticipantLimit()) {
            event.setParticipantLimit(dto.getParticipantLimit());
        } else {
            event.setParticipantLimit(0L);
        }

        if (dto.hasRequestModeration()) {
            event.setRequestModeration(dto.getRequestModeration());
        } else {
            event.setRequestModeration(Boolean.TRUE);
        }

        event.setPublishedOn(LocalDateTime.now());
        event.setState(State.PENDING);
        event.setTitle(dto.getTitle());
        event.setViews(0L);

        return event;
    }

    public static List<EventFullDto> mapToListFullEventDto(List<Event> listEntity) {
        return listEntity.stream().map(EventMapper::mapToFullEventDto).collect(Collectors.toList());
    }

    public static List<EventShortDto> mapToListShortEventDto(Collection<Event> listEntity) {
        if (listEntity == null) {
            return Collections.emptyList();
        }
        return listEntity.stream()
                .map(EventMapper::mapToShortEventDto)
                .collect(Collectors.toList());
    }
}
