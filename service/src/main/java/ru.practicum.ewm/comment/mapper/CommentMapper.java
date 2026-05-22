package ru.practicum.ewm.comment.mapper;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.mapper.UserMapper;

import java.time.LocalDateTime;

public class CommentMapper {
    public static Comment mapToComment(NewCommentDto request, Event event, User author) {
        Comment comment = new Comment();

        comment.setText(request.getText());
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        comment.setUpdated(LocalDateTime.now());
        return comment;
    }

    public static CommentDto mapToCommentDto(Comment comment) {
        CommentDto dto = new CommentDto();

        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setEvent(comment.getEvent().getId());
        dto.setAuthor(UserMapper.mapToUserDto(comment.getAuthor()));
        dto.setCreated(comment.getCreated());
        dto.setUpdated(comment.getUpdated());

        return dto;
    }

    public static Comment updateFields(UpdateCommentDto request, Comment comment) {
        if (request.getText() != null) {
            comment.setText(request.getText());
            comment.setUpdated(LocalDateTime.now());
        }

        return comment;
    }
}
