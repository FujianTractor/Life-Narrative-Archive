CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS archives (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    age INTEGER NOT NULL,
    hometown VARCHAR(128),
    community VARCHAR(128),
    role VARCHAR(128),
    summary TEXT,
    wish TEXT,
    tone VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS archive_tags (
    archive_id VARCHAR(36) NOT NULL,
    tag VARCHAR(64) NOT NULL,
    CONSTRAINT fk_archive_tags_archive FOREIGN KEY (archive_id) REFERENCES archives (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS archive_supporters (
    archive_id VARCHAR(36) NOT NULL,
    supporter VARCHAR(64) NOT NULL,
    CONSTRAINT fk_archive_supporters_archive FOREIGN KEY (archive_id) REFERENCES archives (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS archive_timelines (
    id VARCHAR(36) PRIMARY KEY,
    archive_id VARCHAR(36) NOT NULL,
    year_label VARCHAR(16) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_archive_timelines_archive FOREIGN KEY (archive_id) REFERENCES archives (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assets (
    id VARCHAR(36) PRIMARY KEY,
    archive_id VARCHAR(36) NOT NULL,
    asset_type VARCHAR(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    url_path VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assets_archive FOREIGN KEY (archive_id) REFERENCES archives (id) ON DELETE CASCADE
);
