package ru.practicum.ewm.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LocationDto {
    @NotNull(message = "Широта должна быть указана")
    Float lat;

    @NotNull(message = "Долгота должна быть указана")
    Float lon;
}
