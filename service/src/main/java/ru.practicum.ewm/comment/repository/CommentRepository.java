package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE " +
            "(:text IS NULL OR LOWER(c.text) LIKE LOWER(CONCAT('%', :text, '%'))) AND " +
            "(:users IS NULL OR c.author.id IN :users) AND " +
            "(:events IS NULL OR c.event.id IN :events) AND " +
            "(cast(:rangeStart as timestamp) IS NULL OR c.created >= :rangeStart) AND " +
            "(cast(:rangeEnd as timestamp) IS NULL OR c.created <= :rangeEnd)")
    List<Comment> findAdminComments(
            @Param("text") String text,
            @Param("users") List<Long> users,
            @Param("events") List<Long> events,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    List<Comment> findAllByAuthorId(Long userId, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(Long comId, Long userId);

    List<Comment> findAllByEventId(Long eventId, Pageable pageable);

    Long countByEventId(Long eventId);

    @Query("SELECT c.event.id, COUNT(c) FROM Comment c WHERE c.event.id IN :eventIds GROUP BY c.event.id")
    List<Object[]> countCommentsByEventIds(@Param("eventIds") List<Long> eventIds);
}
