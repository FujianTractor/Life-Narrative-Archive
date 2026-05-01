import { computed, ref } from "vue";
import { defineStore } from "pinia";

import {
  appendTimeline as appendTimelineRequest,
  createArchive as createArchiveRequest,
  fetchArchiveDetail,
  fetchArchives,
  generateSummaryFromDocument as generateSummaryFromDocumentRequest,
  updateArchive as updateArchiveRequest,
  uploadArchiveImage as uploadArchiveImageRequest,
  uploadArchiveVideo as uploadArchiveVideoRequest,
} from "@/services/archiveService";
import type {
  ArchiveCreatePayload,
  ArchiveDetail,
  ArchiveSummary,
  ArchiveTimelinePayload,
  ArchiveUpdatePayload,
} from "@/types/api";

export const useArchiveStore = defineStore("archive", () => {
  const archives = ref<ArchiveSummary[]>([]);
  const overview = ref<Record<string, number>>({});
  const selectedId = ref<string | null>(null);
  const selectedArchive = ref<ArchiveDetail | null>(null);
  const query = ref("");
  const loading = ref(false);

  const filteredArchives = computed(() => {
    const keyword = query.value.trim().toLowerCase();
    if (!keyword) {
      return archives.value;
    }

    return archives.value.filter((archive) => {
      const haystack = [
        archive.name,
        archive.community,
        archive.hometown,
        archive.role,
        archive.summary,
        ...archive.tags,
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(keyword);
    });
  });

  async function loadArchives() {
    loading.value = true;
    try {
      const data = await fetchArchives();
      archives.value = data.elders;
      overview.value = data.overview ?? {};

      if (data.elders.length === 0) {
        selectedId.value = null;
        selectedArchive.value = null;
        return;
      }

      if (!selectedId.value || !data.elders.some((archive) => archive.id === selectedId.value)) {
        selectedId.value = data.elders[0].id;
      }
    } finally {
      loading.value = false;
    }
  }

  async function selectArchive(archiveId: string) {
    selectedId.value = archiveId;
    loading.value = true;
    try {
      const data = await fetchArchiveDetail(archiveId);
      selectedArchive.value = data.elder;
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  async function createArchive(payload: ArchiveCreatePayload) {
    loading.value = true;
    try {
      const data = await createArchiveRequest(payload);
      await loadArchives();
      await selectArchive(data.elder.id);
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  async function updateArchive(payload: ArchiveUpdatePayload) {
    const archiveId = assertSelectedArchive();

    loading.value = true;
    try {
      const data = await updateArchiveRequest(archiveId, payload);
      selectedArchive.value = data.elder;
      await loadArchives();
      await selectArchive(archiveId);
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  async function appendTimeline(payload: ArchiveTimelinePayload) {
    const archiveId = assertSelectedArchive();

    loading.value = true;
    try {
      const data = await appendTimelineRequest(archiveId, payload);
      selectedArchive.value = data.elder;
      await loadArchives();
      await selectArchive(archiveId);
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  async function generateSummaryFromDocument(file: File) {
    const archiveId = assertSelectedArchive();

    loading.value = true;
    try {
      const data = await generateSummaryFromDocumentRequest(archiveId, file);
      selectedArchive.value = data.elder;
      await loadArchives();
      await selectArchive(archiveId);
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  async function uploadImages(files: File[]) {
    const archiveId = assertSelectedArchive();
    if (!files.length) {
      return selectedArchive.value;
    }

    loading.value = true;
    try {
      let latest: ArchiveDetail | null = selectedArchive.value;
      for (const file of files) {
        const data = await uploadArchiveImageRequest(archiveId, file);
        latest = data.elder;
      }
      selectedArchive.value = latest;
      await loadArchives();
      await selectArchive(archiveId);
      return latest;
    } finally {
      loading.value = false;
    }
  }

  async function uploadVideo(file: File) {
    const archiveId = assertSelectedArchive();

    loading.value = true;
    try {
      const data = await uploadArchiveVideoRequest(archiveId, file);
      selectedArchive.value = data.elder;
      await loadArchives();
      await selectArchive(archiveId);
      return data.elder;
    } finally {
      loading.value = false;
    }
  }

  function assertSelectedArchive() {
    if (!selectedId.value) {
      throw new Error("请先选择一份档案");
    }
    return selectedId.value;
  }

  return {
    archives,
    overview,
    selectedId,
    selectedArchive,
    query,
    loading,
    filteredArchives,
    loadArchives,
    selectArchive,
    createArchive,
    updateArchive,
    appendTimeline,
    generateSummaryFromDocument,
    uploadImages,
    uploadVideo,
  };
});
