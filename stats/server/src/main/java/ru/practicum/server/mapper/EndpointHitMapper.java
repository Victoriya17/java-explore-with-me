package ru.practicum.server.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.server.model.EndpointHit;

public class EndpointHitMapper {
    public static EndpointHitDto mapToEndpointHitDto(EndpointHit endpoint) {
        EndpointHitDto dto = new EndpointHitDto();
        dto.setId(endpoint.getId());
        dto.setApp(endpoint.getApp());
        dto.setUri(endpoint.getUri());
        dto.setIp(endpoint.getIp());
        dto.setTimestamp(endpoint.getTimestamp());
        return dto;
    }

    public static EndpointHit mapToEndpointHit(EndpointHitDto dto) {
        EndpointHit endpoint = new EndpointHit();
        endpoint.setId(dto.getId());
        endpoint.setApp(dto.getApp());
        endpoint.setUri(dto.getUri());
        endpoint.setIp(dto.getIp());
        endpoint.setTimestamp(dto.getTimestamp());
        return endpoint;
    }
}
