package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.enums.ParticipationRequestStatus;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public Collection<ParticipationRequestDto> findAllRequests(Long userId) {
        findUserById(userId);

        return RequestMapper.mapToListDto(requestRepository.findAllByRequesterId(userId));
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.debug("Создание запроса от текущего пользователя на участие в событии {}", eventId);
        if (userId == null || eventId == null) {
            throw new BadRequestException("Не заданы ID пользователя или события");
        }

        Event event = findEventWithLockById(eventId);
        User user = findUserById(userId);

        validateAddRequest(event, user, eventId, userId);

        if (event.getRequestModeration().equals(Boolean.FALSE)) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        Request request = RequestMapper.mapToRequest(event, user);
        request = requestRepository.save(request);

        log.info("Сохраняем данные о запросе с ID {} на событие {} созданном пользователем {}",
                request.getId(), event.getTitle(), user.getName());
        return RequestMapper.mapToRequestDto(request);
    }

    private void validateAddRequest(Event event, User user, Long eventId, Long userId) {
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException(String.format("Запрос на событие с ID " + eventId + "от пользователя с ID " +
                    userId + " уже существует"));
        }

        if (event.getInitiator().equals(user)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() != 0L && event.getParticipantLimit().equals(event.getConfirmedRequests())) {
            throw new ConflictException("Достигнут лимит запросов на участие в событии");
        }
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Запрос с ID " + requestId + " пользователя " +
                        "c ID " + userId + " не найден")));
        Event event = findEventWithLockById(request.getEvent().getId());

        if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);
        return RequestMapper.mapToRequestDto(requestRepository.save(request));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID " + userId + " не найден")));
    }

    private Event findEventWithLockById(Long eventId) {
        return eventRepository.findEventWithLockById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID " + eventId + " не найдено")));
    }
}
