package ru.practicum.ewm.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequestDto {
    @NotBlank
    @Size(min = 1, max = 2000)
    private String text;

    @Positive(message = "ID события должен быть положительным числом")
    private Long eventId;

    @Positive(message = "ID комментария должен быть положительным числом")
    private Long commentId;
}
