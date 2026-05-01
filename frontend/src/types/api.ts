export interface UserProfile {
  id: string;
  username: string;
  displayName: string;
}

export interface AuthResponse {
  accessToken: string;
  user: UserProfile;
}

export type ArchiveTone = "amber" | "jade" | "rose";

export interface ArchiveCreatePayload {
  name: string;
  age: number;
  hometown: string;
  community: string;
  role: string;
  summary: string;
  wish: string;
  tags: string[];
  supporters: string[];
  tone: ArchiveTone;
}

export type ArchiveUpdatePayload = ArchiveCreatePayload;

export interface ArchiveTimelinePayload {
  year: string;
  title: string;
  description: string;
}

export interface ArchiveAsset {
  name: string;
  url?: string;
}

export interface ArchiveSummary {
  id: string;
  name: string;
  age: number;
  hometown: string;
  community: string;
  role: string;
  summary: string;
  wish: string;
  tags: string[];
  supporters: string[];
  tone: string;
  updatedAt: string;
}

export interface TimelineEntry {
  year: string;
  title: string;
  description: string;
}

export interface ArchiveDetail extends ArchiveSummary {
  timeline?: TimelineEntry[];
  assets?: {
    images?: ArchiveAsset[];
    videos?: ArchiveAsset[];
  };
}

export interface ArchiveListResponse {
  elders: ArchiveSummary[];
  overview?: Record<string, number>;
  updatedAt?: string;
}

export interface ArchiveDetailResponse {
  elder: ArchiveDetail;
}
