package com.lifenarrative.archive.controller;

import com.lifenarrative.archive.dto.ArchiveDtos.AppendTimelineRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveDetailResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveListResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.CreateArchiveRequest;
import com.lifenarrative.archive.service.ArchiveService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PutMapping("/{archiveId}")
    public ArchiveDetailResponse updateArchive(
            @PathVariable String archiveId,
            @Valid @RequestBody CreateArchiveRequest request
    ) {
        return archiveService.updateArchive(archiveId, request);
    }

    @PostMapping("/{archiveId}/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse appendTimeline(
            @PathVariable String archiveId,
            @Valid @RequestBody AppendTimelineRequest request
    ) {
        return archiveService.appendTimeline(archiveId, request);
    }

    @PostMapping(path = "/{archiveId}/summary-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchiveDetailResponse generateSummaryFromDocument(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return archiveService.generateSummaryFromDocument(archiveId, file);
    }

    @PostMapping(path = "/{archiveId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse uploadImage(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return archiveService.uploadImage(archiveId, file);
    }

    @PostMapping(path = "/{archiveId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse uploadVideo(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return archiveService.uploadVideo(archiveId, file);
    }
}
