package com.lifenarrative.archive.service;

import com.lifenarrative.archive.dto.ArchiveDtos.AppendTimelineRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveDetail;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveDetailResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveListResponse;
import com.lifenarrative.archive.dto.ArchiveDtos.ArchiveSummary;
import com.lifenarrative.archive.dto.ArchiveDtos.AssetBlock;
import com.lifenarrative.archive.dto.ArchiveDtos.CreateArchiveRequest;
import com.lifenarrative.archive.dto.ArchiveDtos.TimelineEntry;
import com.lifenarrative.archive.entity.ArchiveEntity;
import com.lifenarrative.archive.entity.ArchiveTimelineEntity;
import com.lifenarrative.archive.entity.AssetEntity;
import com.lifenarrative.archive.exception.ResourceNotFoundException;
import com.lifenarrative.archive.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ArchiveService {

    private static final String DEFAULT_TONE = "amber";

    private final ArchiveRepository archiveRepository;

    public ArchiveService(ArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
    }

    @Transactional(readOnly = true)
    public ArchiveListResponse listArchives() {
        List<ArchiveEntity> archives = archiveRepository.findAllByOrderByUpdatedAtDesc();
        List<ArchiveSummary> elders = archives.stream()
                .map(this::toSummary)
                .toList();

        return new ArchiveListResponse(
                elders,
                Map.of(
                        "totalArchives", elders.size(),
                        "activeCommunities", (int) archives.stream()
                                .map(ArchiveEntity::getCommunity)
                                .filter(value -> value != null && !value.isBlank())
                                .distinct()
                                .count(),
                        "totalTimelineEvents", archives.stream().mapToInt(archive -> archive.getTimelines().size()).sum(),
                        "totalMediaAssets", archives.stream().mapToInt(archive -> archive.getAssets().size()).sum(),
                        "totalSupportLinks", archives.stream().mapToInt(archive -> archive.getSupporters().size()).sum()
                ),
                toIsoString(Instant.now())
        );
    }

    @Transactional(readOnly = true)
    public ArchiveDetailResponse getArchive(String archiveId) {
        return new ArchiveDetailResponse(toDetail(findArchive(archiveId)));
    }

    @Transactional
    public ArchiveDetailResponse createArchive(CreateArchiveRequest request) {
        ArchiveEntity archive = new ArchiveEntity();
        archive.setName(request.name().trim());
        archive.setAge(request.age());
        archive.setHometown(normalizeText(request.hometown()));
        archive.setCommunity(normalizeText(request.community()));
        archive.setRole(normalizeText(request.role()));
        archive.setSummary(normalizeText(request.summary()));
        archive.setWish(normalizeText(request.wish()));
        archive.setTone(normalizeTone(request.tone()));
        archive.setTags(toLinkedSet(request.tags()));
        archive.setSupporters(toLinkedSet(request.supporters()));

        ArchiveEntity savedArchive = archiveRepository.save(archive);
        return new ArchiveDetailResponse(toDetail(savedArchive));
    }

    @Transactional
    public ArchiveDetailResponse appendTimeline(String archiveId, AppendTimelineRequest request) {
        ArchiveEntity archive = findArchive(archiveId);

        ArchiveTimelineEntity timeline = new ArchiveTimelineEntity();
        timeline.setYearLabel(request.year().trim());
        timeline.setTitle(request.title().trim());
        timeline.setDescription(request.description().trim());
        timeline.setSortOrder(archive.getTimelines().size() + 1);
        archive.addTimeline(timeline);

        ArchiveEntity savedArchive = archiveRepository.save(archive);
        return new ArchiveDetailResponse(toDetail(savedArchive));
    }

    private ArchiveEntity findArchive(String archiveId) {
        return archiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Archive not found"));
    }

    private ArchiveSummary toSummary(ArchiveEntity archive) {
        return new ArchiveSummary(
                archive.getId(),
                archive.getName(),
                archive.getAge(),
                normalizeText(archive.getHometown()),
                normalizeText(archive.getCommunity()),
                normalizeText(archive.getRole()),
                normalizeText(archive.getSummary()),
                normalizeText(archive.getWish()),
                new ArrayList<>(archive.getTags()),
                new ArrayList<>(archive.getSupporters()),
                normalizeTone(archive.getTone()),
                toIsoString(archive.getUpdatedAt())
        );
    }

    private ArchiveDetail toDetail(ArchiveEntity archive) {
        List<TimelineEntry> timeline = archive.getTimelines().stream()
                .map(entry -> new TimelineEntry(entry.getYearLabel(), entry.getTitle(), entry.getDescription()))
                .toList();

        List<Map<String, String>> images = archive.getAssets().stream()
                .filter(asset -> "image".equalsIgnoreCase(asset.getAssetType()))
                .map(this::toAssetView)
                .toList();

        List<Map<String, String>> videos = archive.getAssets().stream()
                .filter(asset -> "video".equalsIgnoreCase(asset.getAssetType()))
                .map(this::toAssetView)
                .toList();

        return new ArchiveDetail(
                archive.getId(),
                archive.getName(),
                archive.getAge(),
                normalizeText(archive.getHometown()),
                normalizeText(archive.getCommunity()),
                normalizeText(archive.getRole()),
                normalizeText(archive.getSummary()),
                normalizeText(archive.getWish()),
                new ArrayList<>(archive.getTags()),
                new ArrayList<>(archive.getSupporters()),
                normalizeTone(archive.getTone()),
                toIsoString(archive.getUpdatedAt()),
                timeline,
                new AssetBlock(images, videos)
        );
    }

    private Map<String, String> toAssetView(AssetEntity asset) {
        return Map.of(
                "name", asset.getName(),
                "url", asset.getUrlPath()
        );
    }

    private Set<String> toLinkedSet(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            String item = normalizeText(value);
            if (!item.isBlank()) {
                normalized.add(item);
            }
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return DEFAULT_TONE;
        }
        String normalized = tone.trim().toLowerCase(Locale.ROOT);
        if ("amber".equals(normalized) || "jade".equals(normalized) || "rose".equals(normalized)) {
            return normalized;
        }
        return DEFAULT_TONE;
    }

    private String toIsoString(Instant value) {
        return value.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}