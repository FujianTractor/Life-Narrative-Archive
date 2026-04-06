package com.lifenarrative.archive.service;

import com.lifenarrative.archive.dto.RagDtos.RagArchiveRef;
import com.lifenarrative.archive.dto.RagDtos.RagQueryRequest;
import com.lifenarrative.archive.dto.RagDtos.RagQueryResponse;
import com.lifenarrative.archive.dto.RagDtos.RagSource;
import com.lifenarrative.archive.rag.FaissIndexManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final FaissIndexManager faissIndexManager;

    public RagService(FaissIndexManager faissIndexManager) {
        this.faissIndexManager = faissIndexManager;
    }

    public RagQueryResponse query(RagQueryRequest request) {
        return new RagQueryResponse(
                "This is the scaffolded Spring Boot RAG response. Retrieval and generation will be connected in a later phase.",
                new RagArchiveRef(request.archiveId(), "Zhang Guilan"),
                List.of(
                        new RagSource("story summary", "This response still uses a scaffolded retrieval chain.", 0.12),
                        new RagSource("integration mode", faissIndexManager.integrationMode(), 0.24)
                ),
                List.of("archive chunks", "embedding", "faiss retrieval", "spring ai / langchain4j answer")
        );
    }
}