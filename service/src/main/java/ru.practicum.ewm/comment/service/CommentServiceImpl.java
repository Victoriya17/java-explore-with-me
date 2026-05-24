package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.AdminCommentParams;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.CommentRequestDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventService eventService;

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long comId) {
        log.debug("Удаление комментария c ID {} администратором", comId);
        Comment comment = findComment(comId);

        log.debug("Удален комментарий {}", comment.getId());
        commentRepository.delete(comment);
    }

    @Override
    public Collection<CommentDto> getAllCommentsByAdmin(AdminCommentParams params) {
        log.debug("Поиск комментариев администратором: text={}, users={}, events={}",
                params.getText(), params.getUsers(), params.getEvents());

        if (params.getRangeStart() != null && params.getRangeEnd() != null) {
            if (params.getRangeStart().isAfter(params.getRangeEnd())) {
                throw new BadRequestException("rangeStart не может быть позже rangeEnd");
            }
        }

        Pageable pageable = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by("id").ascending()
        );

        List<Comment> comments = commentRepository.findAdminComments(
                params.getText(),
                params.getUsers(),
                params.getEvents(),
                params.getRangeStart(),
                params.getRangeEnd(),
                pageable
        );

        log.info("Найдено {} комментариев для администратора", comments.size());

        return comments.stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, CommentRequestDto request) {
        log.debug("Создание нового комментария текущим пользователем");

        if (request.getEventId() == null) {
            throw new BadRequestException("eventId должен быть указан в теле запроса");
        }

        Event event = eventService.findEventById(request.getEventId());

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Нельзя оставить комментарий к неопубликованному событию");
        }

        Comment comment = CommentMapper.mapToComment(request, event, userService.findUserById(userId));

        comment = commentRepository.save(comment);

        log.info("Сохранение комментария в базу данных");
        return CommentMapper.mapToCommentDto(comment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, CommentRequestDto request) {
        log.debug("Обновление комментария пользователем");

        if (request.getCommentId() == null) {
            throw new BadRequestException("commentId должен быть указан в теле запроса");
        }

        Comment existingComment = findComment(request.getCommentId());

        if (!existingComment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Вы не можете редактировать чужой комментарий");
        }

        CommentMapper.updateFields(request, existingComment);
        existingComment.setUpdated(LocalDateTime.now());

        existingComment = commentRepository.save(existingComment);

        log.info("Комментарий с ID {} успешно обновлен", existingComment.getId());
        return CommentMapper.mapToCommentDto(existingComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long comId) {
        log.debug("Удаление комментария c ID {}", comId);
        Comment comment = findComment(comId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Вы не можете удалить чужой комментарий");
        }

        log.info("Комментарий с ID {} успешно удален", comId);
        commentRepository.delete(comment);
    }

    @Override
    public Collection<CommentDto> getAllUserComments(Long userId, Integer from, Integer size) {
        log.debug("Получение всех комментариев пользователя с ID {}: from={}, size={}", userId, from, size);

        Pageable pageable = PageRequest.of(
                from / size,
                size,
                Sort.by("created").descending()
        );

        List<Comment> comments = commentRepository.findAllByAuthorId(userId, pageable);

        return comments.stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getCommentById(Long userId, Long comId) {
        Comment comment = commentRepository.findByIdAndAuthorId(comId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID %d пользователя c ID %d не " +
                        "найден", comId, userId)));

        log.info("Комментарий {} пользователя {} найден", comId, userId);
        return CommentMapper.mapToCommentDto(comment);
    }

    @Override
    public Collection<CommentDto> getCommentsByEvent(Long eventId, Integer from, Integer size) {
        log.debug("Получение всех комментариев к событию с ID {}: from={}, size={}", eventId, from, size);

        eventService.findEventById(eventId);

        Pageable pageable = PageRequest.of(
                from / size,
                size,
                Sort.by("created").descending()
        );

        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);

        return comments.stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getPublicCommentById(Long comId) {
        Comment comment = findComment(comId);

        log.info("Комментарий с ID {} найден", comId);
        return CommentMapper.mapToCommentDto(comment);
    }

    private Comment findComment(Long comId) {
        return commentRepository.findById(comId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + comId + " не найден"));
    }
}
