package lk.ijse.edu.golankacourier.util;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin wrapper over ModelMapper to centralize mapping configuration.
 *
 * Use: mapper.map(src, DestClass.class)
 * Use: mapper.mapList(list, DestClass.class)
 */
@Component
public class Mapper {

    private final ModelMapper modelMapper;

    public Mapper() {
        this.modelMapper = new ModelMapper();
        // conservative matching strategy to avoid accidental mapping
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public <D> D map(Object source, Class<D> destinationType) {
        if (source == null) return null;
        return modelMapper.map(source, destinationType);
    }

    public <D> List<D> mapList(List<?> source, Class<D> destinationType) {
        if (source == null) return null;
        return source.stream()
                .map(element -> map(element, destinationType))
                .collect(Collectors.toList());
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
