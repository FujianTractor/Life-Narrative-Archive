package com.lifenarrative.archive.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class RagDtos {

    private RagDtos() {
    }

    public record RagQueryRequest(
            @NotBlank String archiveId,
            @NotBlank String question
    ) {
    }

    public record RagSource(
            String section,
            String preview,
            double score
    ) {
    }

    public record RagArchiveRef(
            String id,
            String name
    ) {
    }

    public record RagQueryResponse(
            String answer,
            RagArchiveRef archive,
            List<RagSource> sources,
            List<String> workflow
    ) {
    }
}
