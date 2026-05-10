package com.lifenarrative.archive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class ArchiveDtos {

    private ArchiveDtos() {
    }

    public record ArchiveSummary(
            String id,
            String name,
            int age,
            String hometown,
            String community,
            String role,
            String summary,
            String wish,
            List<String> tags,
            List<String> supporters,
            String tone,
            String updatedAt
    ) {
    }

    public record TimelineEntry(
            String id,
            int year,
            String location,
            String title,
            String description
    ) {
    }

    public record AssetBlock(
            List<Map<String, String>> images,
            List<Map<String, String>> videos
    ) {
    }

    public record ArchiveDetail(
            String id,
            String name,
            int age,
            String hometown,
            String community,
            String role,
            String summary,
            String wish,
            List<String> tags,
            List<String> supporters,
            String tone,
            String updatedAt,
            List<TimelineEntry> timeline,
            AssetBlock assets
    ) {
    }

    public record ArchiveListResponse(
            List<ArchiveSummary> elders,
            Map<String, Integer> overview,
            String updatedAt
    ) {
    }

    public record ArchiveDetailResponse(
            ArchiveDetail elder
    ) {
    }

    public record CreateArchiveRequest(
            @NotBlank String name,
            @NotNull @Min(50) Integer age,
            String hometown,
            String community,
            String role,
            String summary,
            String wish,
            List<String> tags,
            List<String> supporters,
            String tone
    ) {
    }

    public record AppendTimelineRequest(
            @NotBlank String year,
            String location,
            @NotBlank String title,
            @NotBlank String description
    ) {
    }

    public record TimelineUpdateRequest(
            @NotBlank String year,
            String location,
            @NotBlank String title,
            @NotBlank String description
    ) {
    }
}
