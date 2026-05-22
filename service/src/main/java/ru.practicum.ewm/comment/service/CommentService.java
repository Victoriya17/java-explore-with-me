package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.AdminCommentParams;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;

import java.util.Collection;

public interface CommentService {
    void deleteCommentByAdmin(Long comId);

    Collection<CommentDto> getAllCommentsByAdmin(AdminCommentParams params);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto request);

    CommentDto updateComment(Long userId, Long comId, UpdateCommentDto request);

    void deleteCommentByUser(Long userId, Long comId);

    Collection<CommentDto> getAllUserComments(Long userId, Integer from, Integer size);

    CommentDto getCommentById(Long userId, Long comId);

    Collection<CommentDto> getCommentsByEvent(Long eventId, Integer from, Integer size);

    CommentDto getPublicCommentById(Long comId);
}
