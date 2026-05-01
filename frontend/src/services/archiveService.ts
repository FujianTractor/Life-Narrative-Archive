import http from "./http";
import type {
  ArchiveCreatePayload,
  ArchiveDetailResponse,
  ArchiveListResponse,
  ArchiveTimelinePayload,
  ArchiveUpdatePayload,
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

export async function updateArchive(archiveId: string, payload: ArchiveUpdatePayload) {
  const { data } = await http.put<ArchiveDetailResponse>(`/archives/${archiveId}`, payload);
  return data;
}

export async function appendTimeline(archiveId: string, payload: ArchiveTimelinePayload) {
  const { data } = await http.post<ArchiveDetailResponse>(`/archives/${archiveId}/timeline`, payload);
  return data;
}

export async function generateSummaryFromDocument(archiveId: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await http.post<ArchiveDetailResponse>(`/archives/${archiveId}/summary-document`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data;
}

export async function uploadArchiveImage(archiveId: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await http.post<ArchiveDetailResponse>(`/archives/${archiveId}/images`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data;
}

export async function uploadArchiveVideo(archiveId: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await http.post<ArchiveDetailResponse>(`/archives/${archiveId}/videos`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data;
}
