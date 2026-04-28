package ru.practicum.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.model.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    @Query("""
            SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, COUNT(DISTINCT eh.ip))
            FROM EndpointHit eh
            WHERE eh.timestamp BETWEEN :start AND :end
            GROUP BY eh.app, eh.uri
            ORDER BY COUNT(DISTINCT eh.ip) DESC
            """)
    List<ViewStats> getUniqueHits(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, COUNT(eh))
            FROM EndpointHit eh
            WHERE eh.timestamp BETWEEN :start AND :end
            GROUP BY eh.app, eh.uri
            ORDER BY COUNT(eh) DESC
            """)
    List<ViewStats> getHits(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, COUNT(DISTINCT eh.ip))
            FROM EndpointHit eh
            WHERE eh.timestamp BETWEEN :start AND :end
            AND eh.uri IN :uris
            GROUP BY eh.app, eh.uri
            ORDER BY COUNT(DISTINCT eh.ip) DESC
            """)
    List<ViewStats> getUrisUniqueHits(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            SELECT new ru.practicum.server.model.ViewStats(eh.app, eh.uri, COUNT(eh))
            FROM EndpointHit eh
            WHERE eh.timestamp BETWEEN :start AND :end
            AND eh.uri IN :uris
            GROUP BY eh.app, eh.uri
            ORDER BY COUNT(eh) DESC
            """)
    List<ViewStats> getUrisHits(LocalDateTime start, LocalDateTime end, List<String> uris);
}
