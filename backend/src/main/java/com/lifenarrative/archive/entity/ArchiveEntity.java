package com.lifenarrative.archive.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "archives")
public class ArchiveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 128)
    private String hometown;

    @Column(length = 128)
    private String community;

    @Column(length = 128)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String wish;

    @Column(length = 32)
    private String tone;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "archive_tags", joinColumns = @JoinColumn(name = "archive_id"))
    @Column(name = "tag", nullable = false, length = 64)
    private Set<String> tags = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "archive_supporters", joinColumns = @JoinColumn(name = "archive_id"))
    @Column(name = "supporter", nullable = false, length = 64)
    private Set<String> supporters = new LinkedHashSet<>();

    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ArchiveTimelineEntity> timelines = new ArrayList<>();

    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetEntity> assets = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addTimeline(ArchiveTimelineEntity timeline) {
        timelines.add(timeline);
        timeline.setArchive(this);
    }

    public void addAsset(AssetEntity asset) {
        assets.add(asset);
        asset.setArchive(this);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getWish() {
        return wish;
    }

    public void setWish(String wish) {
        this.wish = wish;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getSupporters() {
        return supporters;
    }

    public void setSupporters(Set<String> supporters) {
        this.supporters = supporters;
    }

    public List<ArchiveTimelineEntity> getTimelines() {
        return timelines;
    }

    public List<AssetEntity> getAssets() {
        return assets;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
