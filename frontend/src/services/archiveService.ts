import http from "./http";
import type {
  ArchiveCreatePayload,
  ArchiveDetailResponse,
  ArchiveListResponse,
  ArchiveTimelinePayload,
} from "@/types/api";

export async function fetchArchives() {
  const { data } = await http.get<ArchiveListResponse>("/archives");
  return data;
}

export async function fetchArchiveDetail(archiveId: string) {
  const { data } = await http.get<ArchiveDetailResponse>(`/archives/${archiveId}`);
  return data;
}

export async function createArchive(payload: ArchiveCreatePayload) {
  const { data } = await http.post<ArchiveDetailResponse>("/archives", payload);
  return data;
}

export async function appendTimeline(archiveId: string, payload: ArchiveTimelinePayload) {
  const { data } = await http.post<ArchiveDetailResponse>(`/archives/${archiveId}/timeline`, payload);
  return data;
}