package ru.practicum.server.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ViewStats {
    private String app;
    private String uri;
    private Long hits;
}
