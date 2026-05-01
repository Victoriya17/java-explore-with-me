package ru.practicum.server.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.mapper.EndpointHitMapper;
import ru.practicum.server.mapper.ViewStatsMapper;
import ru.practicum.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    public StatsServiceImpl(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @Override
    @Transactional
    public EndpointHitDto addHit(EndpointHitDto hitDto) {
        log.info("Сохранение запроса из {} к {} с ip {}", hitDto.getApp(), hitDto.getUri(), hitDto.getId());
        return EndpointHitMapper.mapToEndpointHitDto(
                statsRepository.save(EndpointHitMapper.mapToEndpointHit(hitDto)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (uris == null || uris.isEmpty()) {
            if (unique) {
                log.info("Получение статистики обращений без списка uri с {} по {}", start, end);
                return ViewStatsMapper.mapToListDto(statsRepository.getUniqueHits(start, end));
            } else {
                log.info("Получение статистики уникальных обращений без списка uri с {} по {}", start, end);
                return ViewStatsMapper.mapToListDto(statsRepository.getHits(start, end));
            }
        }

        if (unique) {
            log.info("Получение статистики обращений с {} по {}", start, end);
            return ViewStatsMapper.mapToListDto(statsRepository.getUrisUniqueHits(start, end, uris));
        } else {
            log.info("Получение статистики уникальных обращений с {} по {}", start, end);
            return ViewStatsMapper.mapToListDto(statsRepository.getUrisHits(start, end, uris));
        }
    }
}
