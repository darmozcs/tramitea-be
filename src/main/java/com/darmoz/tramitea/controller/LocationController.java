package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.dto.response.LocationResponse;
import com.darmoz.tramitea.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Ubicaciones", description = "Países y provincias habilitados. Lectura pública.")
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "Ubicaciones visibles",
            description = "Países activos y las provincias activas de esos países, por nombre. Una provincia apunta a su país con `parentId`.")
    public List<LocationResponse> list() {
        return locationService.listVisible();
    }
}
