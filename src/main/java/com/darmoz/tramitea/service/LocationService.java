package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.response.LocationResponse;
import com.darmoz.tramitea.entity.Location;
import com.darmoz.tramitea.entity.LocationType;
import com.darmoz.tramitea.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Países y provincias. Una ubicación es visible si está activa y, si es
 * provincia, su país también: lo que esté en una ubicación no visible no
 * sale en la API pública.
 */
@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> listVisible() {
        return locationRepository.findVisible().stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<UUID> visibleIds() {
        return locationRepository.findVisible().stream()
                .map(Location::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Ubicaciones que cubre un filtro: un país trae sus provincias, y una
     * provincia trae su país (los trámites nacionales también aplican ahí).
     */
    @Transactional(readOnly = true)
    public Set<UUID> expandFilter(Collection<UUID> locationIds) {
        Set<UUID> expanded = new HashSet<>(locationIds);
        List<Location> selected = locationRepository.findAllById(locationIds);
        selected.stream()
                .filter(l -> l.getType() == LocationType.PROVINCE)
                .forEach(l -> expanded.add(l.getParentId()));
        List<UUID> countries = selected.stream()
                .filter(l -> l.getType() == LocationType.COUNTRY)
                .map(Location::getId)
                .toList();
        if (!countries.isEmpty()) {
            locationRepository.findByParentIdIn(countries).forEach(l -> expanded.add(l.getId()));
        }
        return expanded;
    }

    /** La ubicación si existe y es visible; si no, null. */
    @Transactional(readOnly = true)
    public Location findVisible(UUID locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId)
                .filter(l -> visibleIds().contains(l.getId()))
                .orElse(null);
    }
}
