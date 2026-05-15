package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.location.dto.LocationDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class UpdateEventBaseRequest {
    @Size(min = 20, max = 2000)
    String annotation;

    @Positive(message = "ID категории не может быть меньше 0")
    Long category;

    @Size(min = 20, max = 7000)
    String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    @Valid
    LocationDto location;

    Boolean paid;

    @PositiveOrZero(message = "Лимит участников не может быть отрицательным числом")
    Long participantLimit;

    Boolean requestModeration;

    @Size(min = 3, max = 120)
    String title;

    public boolean hasAnnotation() {
        return this.annotation != null;
    }

    public boolean hasCategory() {
        return this.category != null;
    }

    public boolean hasDescription() {
        return this.description != null;
    }

    public boolean hasEventDate() {
        return this.eventDate != null;
    }

    public boolean hasLocation() {
        return this.location != null;
    }

    public boolean hasPaid() {
        return this.paid != null;
    }

    public boolean hasParticipantLimit() {
        return this.participantLimit != null;
    }

    public boolean hasRequestModeration() {
        return this.requestModeration != null;
    }

    public boolean hasTitle() {
        return this.title != null;
    }
}
