package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.AdminCommentParams;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.CommentRequestDto;

import java.util.Collection;

public interface CommentService {
    void deleteCommentByAdmin(Long comId);

    Collection<CommentDto> getAllCommentsByAdmin(AdminCommentParams params);

    CommentDto createComment(Long userId, CommentRequestDto request);

    CommentDto updateComment(Long userId, CommentRequestDto request);

    void deleteCommentByUser(Long userId, Long comId);

    Collection<CommentDto> getAllUserComments(Long userId, Integer from, Integer size);

    CommentDto getCommentById(Long userId, Long comId);

    Collection<CommentDto> getCommentsByEvent(Long eventId, Integer from, Integer size);

    CommentDto getPublicCommentById(Long comId);
}
