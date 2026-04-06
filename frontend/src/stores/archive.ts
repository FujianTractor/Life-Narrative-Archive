import { computed, ref } from "vue";
import { defineStore } from "pinia";

import {
  appendTimeline as appendTimelineRequest,
  createArchive as createArchiveRequest,
  fetchArchiveDetail,
  fetchArchives,
} from "@/services/archiveService";
import type {
  ArchiveCreatePayload,
  ArchiveDetail,
  ArchiveSummary,
  ArchiveTimelinePayload,
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
        archive.summary,
        archive.wish,
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

  async function appendTimeline(payload: ArchiveTimelinePayload) {
    if (!selectedId.value) {
      throw new Error("请先选择一份档案");
    }

    loading.value = true;
    try {
      const data = await appendTimelineRequest(selectedId.value, payload);
      selectedArchive.value = data.elder;
      await loadArchives();
      return data.elder;
    } finally {
      loading.value = false;
    }
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
    appendTimeline,
  };
});