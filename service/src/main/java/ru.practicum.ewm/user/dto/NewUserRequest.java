package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewUserRequest {
    @NotBlank(message = "Имя пользователя должно быть указано")
    @Size(min = 2, max = 250)
    private String name;

    @NotBlank(message = "E-mail должен быть указан")
    @Email(message = "E-mail должен быть в формате name@mail.ru")
    @Size(min = 6, max = 254)
    private String email;
}
