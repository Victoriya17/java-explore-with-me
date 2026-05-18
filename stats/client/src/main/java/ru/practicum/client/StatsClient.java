package ru.practicum.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient extends BaseClient {
    private final String appName;

    @Autowired
    public StatsClient(@Value("${stats.url:http://stats-server:9090}") String serverUrl,
                       @Value("${spring.application.name:ewm-main-service}") String appName,
                       RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(org.springframework.http.client.SimpleClientHttpRequestFactory::new)
                        .build()
        );
        this.appName = appName;
    }

    public ResponseEntity<Object> get(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null) {
            for (String uri : uris) {
                uriBuilder.queryParam("uris", uri);
            }
        }

        return get(uriBuilder.build().toUriString(), null);
    }

    public ResponseEntity<Object> save(HttpServletRequest request) {
        EndpointHitDto hitDto = new EndpointHitDto(
                null,
                appName,
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );
        return post("/hit", null, hitDto);
    }
}