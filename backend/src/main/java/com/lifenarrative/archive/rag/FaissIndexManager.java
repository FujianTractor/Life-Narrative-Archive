package com.lifenarrative.archive.rag;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class FaissIndexManager {

    public Path resolveArchiveIndexPath(String archiveId) {
        return Path.of("data", "faiss", archiveId);
    }

    public String integrationMode() {
        return "faiss-placeholder";
    }
}
