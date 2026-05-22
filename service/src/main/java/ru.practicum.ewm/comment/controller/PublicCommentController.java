package ru.practicum.ewm.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.Collection;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/events/{eventId}/comments")
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    public Collection<CommentDto> getCommentsByEvent(@PathVariable @Positive Long eventId,
                                                     @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                     @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Публичный запрос GET /comments/events/{} с параметрами from={}, size={}", eventId, from, size);
        return commentService.getCommentsByEvent(eventId, from, size);
    }

    @GetMapping("/{comId}")
    public CommentDto getPublicCommentById(@PathVariable @Positive Long comId) {
        log.info("Публичный запрос GET /comments/{} на получение комментария", comId);
        return commentService.getPublicCommentById(comId);
    }
}
