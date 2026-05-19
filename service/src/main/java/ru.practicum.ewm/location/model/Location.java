package ru.practicum.ewm.location.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "locations")
public class Location {
    private Float lat;
    private Float lon;
}
