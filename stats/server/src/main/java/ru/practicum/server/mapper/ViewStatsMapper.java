package ru.practicum.server.mapper;

import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.model.ViewStats;

import java.util.List;
import java.util.stream.Collectors;

public class ViewStatsMapper {
    public static ViewStatsDto mapToViewStatsDto(ViewStats stats) {
        ViewStatsDto dto = new ViewStatsDto();
        dto.setApp(stats.getApp());
        dto.setUri(stats.getUri());
        dto.setHits(stats.getHits());
        return dto;
    }

    public static ViewStats mapToViewStats(ViewStatsDto dto) {
        ViewStats stats = new ViewStats();
        stats.setApp(dto.getApp());
        stats.setUri(dto.getUri());
        stats.setHits(dto.getHits());
        return stats;
    }

    public static List<ViewStatsDto> mapToListDto(List<ViewStats> listDto) {
        return listDto.stream().map(ViewStatsMapper::mapToViewStatsDto).collect(Collectors.toList());
    }
}
