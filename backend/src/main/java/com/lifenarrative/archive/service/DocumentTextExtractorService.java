package com.lifenarrative.archive.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Service
public class DocumentTextExtractorService {

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String extension = getExtension(fileName);

        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "docx" -> extractDocx(inputStream);
                case "doc" -> extractDoc(inputStream);
                default -> throw new IllegalArgumentException("仅支持上传 DOCX 或 DOC 文档");
            };
        }
    }

    private String extractDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return normalizeWhitespace(extractor.getText());
        }
    }

    private String extractDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return normalizeWhitespace(extractor.getText());
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
