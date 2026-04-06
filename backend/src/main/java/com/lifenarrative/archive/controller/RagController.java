package com.lifenarrative.archive.controller;

import com.lifenarrative.archive.dto.RagDtos.RagQueryRequest;
import com.lifenarrative.archive.dto.RagDtos.RagQueryResponse;
import com.lifenarrative.archive.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public RagQueryResponse query(@Valid @RequestBody RagQueryRequest request) {
        return ragService.query(request);
    }
}
