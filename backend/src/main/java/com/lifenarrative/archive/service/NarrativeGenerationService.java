package com.lifenarrative.archive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NarrativeGenerationService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(19\\d{2}|20\\d{2})");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public NarrativeGenerationService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public GeneratedNarrative generateNarrative(String archiveName, String role, String documentText) {
        String normalizedText = documentText == null ? "" : documentText.trim();
        if (normalizedText.isBlank()) {
            throw new IllegalArgumentException("No readable content found in the uploaded document.");
        }

        try {
            return generateWithModel(archiveName, role, normalizedText);
        } catch (Exception ignored) {
            return buildFallbackNarrative(archiveName, normalizedText);
        }
    }

    private GeneratedNarrative generateWithModel(String archiveName, String role, String documentText) throws Exception {
        String prompt = """
                You are an archive assistant.
                Based on the provided document, generate a Chinese summary and timeline.

                Output rules:
                1. Return strict JSON only (no markdown, no extra explanation).
                2. Use this exact schema:
                {
                  "summary": "Chinese summary text",
                  "timeline": [
                    { "year": "year or stage", "title": "event title", "description": "event description" }
                  ]
                }
                3. Keep all values in Chinese.
                4. Summary should be concise but complete (around 120-220 Chinese chars when possible).
                5. Timeline should include 3-6 items in chronological order.
                6. If no explicit year is present, use stage-like labels.
                7. Do not fabricate unsupported facts.

                Person name: %s
                Person role: %s

                Document content:
                %s
                """.formatted(
                safeText(archiveName, "Unknown person"),
                safeText(role, "Not provided"),
                truncate(documentText, 12000)
        );

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Model returned empty content.");
        }

        JsonNode root = objectMapper.readTree(stripCodeFence(response));
        String summary = normalizeText(root.path("summary").asText());
        List<TimelineDraft> timeline = new ArrayList<>();

        for (JsonNode item : root.path("timeline")) {
            String year = normalizeText(item.path("year").asText());
            String title = normalizeText(item.path("title").asText());
            String description = normalizeText(item.path("description").asText());
            if (!year.isBlank() && !title.isBlank() && !description.isBlank()) {
                timeline.add(new TimelineDraft(year, title, description));
            }
        }

        if (summary.isBlank() || timeline.isEmpty()) {
            throw new IllegalStateException("Model returned empty summary or timeline.");
        }

        return new GeneratedNarrative(summary, timeline);
    }

    private GeneratedNarrative buildFallbackNarrative(String archiveName, String documentText) {
        List<String> sentences = splitIntoSentences(documentText);
        String summarySeed = String.join(" ", sentences.stream().limit(3).toList()).trim();
        if (summarySeed.isBlank()) {
            summarySeed = truncate(documentText, 180);
        }

        String summary = normalizeText(
                safeText(archiveName, "This person") + " archive document has been imported. "
                        + truncate(summarySeed, 180)
        );

        List<TimelineDraft> timeline = extractTimelineFromText(documentText);
        if (timeline.isEmpty()) {
            timeline = List.of(
                    new TimelineDraft(
                            "Life journey",
                            "Imported document content",
                            truncate(summarySeed.isBlank() ? documentText : summarySeed, 120)
                    )
            );
        }

        return new GeneratedNarrative(summary, timeline);
    }

    private List<TimelineDraft> extractTimelineFromText(String documentText) {
        List<TimelineDraft> timeline = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String sentence : splitIntoSentences(documentText)) {
            Matcher matcher = YEAR_PATTERN.matcher(sentence);
            if (!matcher.find()) {
                continue;
            }

            String year = matcher.group(1);
            String cleanedSentence = normalizeText(sentence);
            if (cleanedSentence.isBlank() || !seen.add(year + cleanedSentence)) {
                continue;
            }

            String title = cleanedSentence.length() > 18
                    ? cleanedSentence.substring(0, 18) + "..."
                    : cleanedSentence;

            timeline.add(new TimelineDraft(year, title, truncate(cleanedSentence, 120)));
            if (timeline.size() >= 6) {
                break;
            }
        }

        return timeline;
    }

    private List<String> splitIntoSentences(String text) {
        String[] parts = text.replace("\r", "\n").split("(?<=[\\u3002\\uff01\\uff1f.!?])|\\n");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String normalized = normalizeText(part);
            if (!normalized.isBlank()) {
                sentences.add(normalized);
            }
        }
        return sentences;
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = normalizeText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String safeText(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    public record GeneratedNarrative(String summary, List<TimelineDraft> timeline) {
    }

    public record TimelineDraft(String year, String title, String description) {
    }
}
