package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Collection<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageRequest).getContent();
        } else {
           users = userRepository.findByIdIn(ids, pageRequest);
        }

        log.info("Найдено {} пользователей для администратора", users.size());
        return users.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        log.debug("Добавление нового пользователя {}", request.getName());
        User newUser = UserMapper.mapToUser(request);

        userRepository.findUserByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new DuplicatedDataException("Эта почта уже используется.");
                });

        try {
            newUser = userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Такой пользователь уже существует");
        }
        log.info("Сохранение данных о пользователе {}", request.getName());
        return UserMapper.mapToUserDto(newUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Удаление пользователя c ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        log.debug("Удаление пользователя {}", user.getName());
        userRepository.delete(user);
    }
}
