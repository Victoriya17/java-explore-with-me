package ru.practicum.ewm.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.Collection;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/users/{userId}/comments")
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable("userId") @Positive Long userId,
                                    @RequestParam Long eventId,
                                    @Valid @RequestBody NewCommentDto request) {
        log.info("Получен запрос POST /user/{}/comments на создание комментария с текстом: {}", userId, request);
        return commentService.createComment(userId, eventId, request);
    }

    @PatchMapping("/{comId}")
    public CommentDto updateComment(@PathVariable("userId") @Positive Long userId,
                                    @PathVariable("comId") @Positive Long comId,
                                    @Valid @RequestBody UpdateCommentDto request) {
        log.info("Получен запрос PATCH /user/{}/comments/{} на обновление комментария с текстом: {}", userId, comId,
                request);
        return commentService.updateComment(userId, comId, request);
    }

    @DeleteMapping("/{comId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("userId") @Positive Long userId,
                              @PathVariable("comId") @Positive Long comId) {
        log.info("Получен запрос DELETE /user/{}/comments/{} на удаление комментария", userId, comId);
        commentService.deleteCommentByUser(userId, comId);
    }

    @GetMapping
    public Collection<CommentDto> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Запрос GET /users/{}/comments (from={}, size={})", userId, from, size);
        return commentService.getAllUserComments(userId, from, size);
    }

    @GetMapping("/{comId}")
    public CommentDto getUserComment(
            @PathVariable Long userId,
            @PathVariable Long comId) {
        log.info("Запрос GET /users/{}/comments/{}", userId, comId);
        return commentService.getCommentById(userId, comId);
    }
}
