package com.lifenarrative.archive.controller;

import com.lifenarrative.archive.dto.ArchiveDtos.AppendTimelineRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveDetailResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveListResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.CreateArchiveRequest;
import com.lifenarrative.archive.service.ArchiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/archives")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping
    public ArchiveListResponse listArchives() {
        return archiveService.listArchives();
    }

    @GetMapping("/{archiveId}")
    public ArchiveDetailResponse getArchive(@PathVariable String archiveId) {
        return archiveService.getArchive(archiveId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse createArchive(@Valid @RequestBody CreateArchiveRequest request) {
        return archiveService.createArchive(request);
    }

    @PostMapping("/{archiveId}/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse appendTimeline(
            @PathVariable String archiveId,
            @Valid @RequestBody AppendTimelineRequest request
    ) {
        return archiveService.appendTimeline(archiveId, request);
    }
}