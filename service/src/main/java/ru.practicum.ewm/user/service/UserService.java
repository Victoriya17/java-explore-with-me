package ru.practicum.ewm.user.service;

import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;

import java.util.Collection;
import java.util.List;

public interface UserService {
    Collection<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    UserDto createUser(NewUserRequest request);

    void deleteUser(Long userId);

    User findUserById(Long userId);
}
