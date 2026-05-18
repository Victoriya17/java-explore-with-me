package ru.practicum.ewm.request.mapper;

import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.enums.ParticipationRequestStatus;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RequestMapper {
    public static Request mapToRequest(Event event, User requester) {
        Request request = new Request();

        request.setCreated(LocalDateTime.now());
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(!event.getRequestModeration() || event.getParticipantLimit().equals(0L) ?
                ParticipationRequestStatus.CONFIRMED : ParticipationRequestStatus.PENDING);

        return request;
    }

    public static ParticipationRequestDto mapToRequestDto(Request request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();

        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setEvent(request.getEvent().getId());
        dto.setRequester(request.getRequester().getId());
        dto.setStatus(request.getStatus());

        return dto;
    }

    public static List<ParticipationRequestDto> mapToListDto(List<Request> listEntity) {
        return listEntity.stream().map(RequestMapper::mapToRequestDto).collect(Collectors.toList());
    }
}
