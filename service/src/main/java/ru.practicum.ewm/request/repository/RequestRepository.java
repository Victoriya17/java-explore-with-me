package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.request.enums.ParticipationRequestStatus;
import ru.practicum.ewm.request.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventId(Long eventId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long userId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    long countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);

    List<Request> findAllByEventIdInAndStatus(List<Long> eventIds, ParticipationRequestStatus status);
}
