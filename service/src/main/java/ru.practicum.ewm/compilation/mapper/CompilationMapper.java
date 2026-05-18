package ru.practicum.ewm.compilation.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.mapper.EventMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {
    public static CompilationDto mapToCompilationDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.getPinned());
        dto.setTitle(compilation.getTitle());
        if (compilation.getEvents() != null) {
            dto.setEvents(new HashSet<>(EventMapper.mapToListShortEventDto(compilation.getEvents())));
        } else {
            dto.setEvents(Collections.emptySet());
        }
        return dto;
    }

    public static Compilation mapToCompilation(NewCompilationDto request, Set<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setPinned(request.getPinned() != null ? request.getPinned() : false);
        compilation.setTitle(request.getTitle());
        compilation.setEvents(events);
        return compilation;
    }

    public static Compilation updateFields(Compilation compilation, UpdateCompilationRequest request,
                                           Set<Event> events) {
        if (request.hasPinned()) {
            compilation.setPinned(request.getPinned());
        }

        if (request.hasTitle()) {
            compilation.setTitle(request.getTitle());
        }

        if (request.hasEvents()) {
            compilation.setEvents(events);
        }

        return compilation;
    }

    public static List<CompilationDto> mapToListDto(List<Compilation> compilations) {
        return compilations.stream().map(CompilationMapper::mapToCompilationDto).toList();
    }
}
