package com.example.api.controller;

import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import com.example.api.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 시설 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityRepository facilityRepository;

    /**
     * 시설 목록 조회
     * GET /api/v1/facilities
     */
    @GetMapping
    public ResponseEntity<List<Facility>> list(@RequestParam(required = false) FacilityType type) {
        List<Facility> facilities;
        if (type != null) {
            facilities = facilityRepository.findAll().stream()
                    .filter(f -> f.getType() == type)
                    .toList();
        } else {
            facilities = facilityRepository.findAll();
        }
        return ResponseEntity.ok(facilities);
    }

    /**
     * 시설 상세 조회
     * GET /api/v1/facilities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Facility> get(@PathVariable Long id) {
        return facilityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
