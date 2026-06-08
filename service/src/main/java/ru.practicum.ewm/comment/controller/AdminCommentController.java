package ru.practicum.ewm.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.AdminCommentParams;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.Collection;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    @DeleteMapping("/{comId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("comId") @Positive Long comId) {
        log.info("Получен запрос DELETE /admin/comments/{} от администратора на удаление комментария", comId);
        commentService.deleteCommentByAdmin(comId);
    }

    @GetMapping
    public Collection<CommentDto> getAllCommentsByAdmin(@Valid AdminCommentParams params) {
        log.info("Получен запрос GET /admin/comments от администратора с параметрами фильтрации: {}", params);
        return commentService.getAllCommentsByAdmin(params);
    }
}
