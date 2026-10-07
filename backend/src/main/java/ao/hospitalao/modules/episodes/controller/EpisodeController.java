package ao.hospitalao.modules.episodes.controller;

import ao.hospitalao.modules.episodes.dto.CreateEpisodeRequest;
import ao.hospitalao.modules.episodes.dto.EpisodeResponse;
import ao.hospitalao.modules.episodes.dto.UpdateEpisodeRequest;
import ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus;
import ao.hospitalao.modules.episodes.service.EpisodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/episodes")
@RequiredArgsConstructor
@Tag(name = "Episodes", description = "Clinical episodes and consultations")
@SecurityRequirement(name = "bearerAuth")
public class EpisodeController {

    private final EpisodeService episodeService;

    @GetMapping
    @Operation(summary = "List episodes with optional filters")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','MANAGER')")
    public ResponseEntity<Page<EpisodeResponse>> findAll(
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) UUID doctorId,
        @RequestParam(required = false) EpisodeStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
            episodeService.findAll(patientId, doctorId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get episode by ID")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','MANAGER')")
    public ResponseEntity<EpisodeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Schedule a new episode")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
    public ResponseEntity<EpisodeResponse> create(
        @Valid @RequestBody CreateEpisodeRequest request
    ) {
        EpisodeResponse created = episodeService.create(request);
        URI uri = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update episode clinical data")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<EpisodeResponse> update(
        @PathVariable UUID id,
        @RequestBody UpdateEpisodeRequest request
    ) {
        return ResponseEntity.ok(episodeService.update(id, request));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Start episode (SCHEDULED → IN_PROGRESS)")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<EpisodeResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeService.start(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete episode (IN_PROGRESS → COMPLETED)")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<EpisodeResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeService.complete(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel episode")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public ResponseEntity<EpisodeResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeService.cancel(id));
    }
}