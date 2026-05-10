package com.lifenarrative.archive.controller;

import com.lifenarrative.archive.dto.ArchiveDtos.AppendTimelineRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveDetailResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveListResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.CreateArchiveRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.TimelineUpdateRequest;
import com.lifenarrative.archive.security.ArchivePrincipal;
import com.lifenarrative.archive.service.ArchiveService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    public ArchiveListResponse listArchives(@AuthenticationPrincipal ArchivePrincipal principal) {
        return archiveService.listArchives(principal.userId());
    }

    @GetMapping("/{archiveId}")
    public ArchiveDetailResponse getArchive(
            @PathVariable String archiveId,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.getArchive(archiveId, principal.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse createArchive(
            @Valid @RequestBody CreateArchiveRequest request,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.createArchive(request, principal.userId());
    }

    @PutMapping("/{archiveId}")
    public ArchiveDetailResponse updateArchive(
            @PathVariable String archiveId,
            @Valid @RequestBody CreateArchiveRequest request,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.updateArchive(archiveId, request, principal.userId());
    }

    @PostMapping("/{archiveId}/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse appendTimeline(
            @PathVariable String archiveId,
            @Valid @RequestBody AppendTimelineRequest request,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.appendTimeline(archiveId, request, principal.userId());
    }

    @PutMapping("/{archiveId}/timeline/{timelineId}")
    public ArchiveDetailResponse updateTimeline(
            @PathVariable String archiveId,
            @PathVariable String timelineId,
            @Valid @RequestBody TimelineUpdateRequest request,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.updateTimeline(archiveId, timelineId, request, principal.userId());
    }

    @DeleteMapping("/{archiveId}/timeline/{timelineId}")
    public ArchiveDetailResponse deleteTimeline(
            @PathVariable String archiveId,
            @PathVariable String timelineId,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) {
        return archiveService.deleteTimeline(archiveId, timelineId, principal.userId());
    }

    @PostMapping(path = "/{archiveId}/summary-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchiveDetailResponse generateSummaryFromDocument(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) throws IOException {
        return archiveService.generateSummaryFromDocument(archiveId, file, principal.userId());
    }

    @PostMapping(path = "/{archiveId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse uploadImage(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) throws IOException {
        return archiveService.uploadImage(archiveId, file, principal.userId());
    }

    @PostMapping(path = "/{archiveId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveDetailResponse uploadVideo(
            @PathVariable String archiveId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal ArchivePrincipal principal
    ) throws IOException {
        return archiveService.uploadVideo(archiveId, file, principal.userId());
    }
}
