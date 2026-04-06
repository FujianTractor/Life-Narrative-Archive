<template>
  <AppShell title="档案工作台">
    <template #actions>
      <button class="secondary-button" type="button" @click="logout">退出</button>
    </template>

    <section class="workspace-shell">
      <aside class="workspace-sidebar panel">
        <div class="workspace-sidebar__header">
          <p class="eyebrow">档案列表</p>
          <input v-model.trim="archiveStore.query" class="search-input" placeholder="搜索姓名、社区、籍贯或标签" />
        </div>

        <div v-if="archiveStore.filteredArchives.length" class="archive-list">
          <ArchiveCard
            v-for="archive in archiveStore.filteredArchives"
            :key="archive.id"
            :archive="archive"
            :active="archive.id === archiveStore.selectedId"
            @click="archiveStore.selectArchive(archive.id)"
          />
        </div>
        <p v-else class="archive-list__empty">
          {{ archiveStore.loading ? "正在加载档案..." : "没有匹配的档案，请调整搜索条件。" }}
        </p>
      </aside>

      <section class="workspace-content panel">
        <div class="workspace-content__header">
          <div>
            <p class="eyebrow">当前会话</p>
            <h2>{{ auth.currentUser?.displayName || auth.currentUser?.username || "未登录" }}</h2>
            <p>本轮已打通创建档案、查看详情和补录时间线，档案统计会随操作即时刷新。</p>
          </div>

          <div class="overview-grid">
            <article v-for="metric in overviewMetrics" :key="metric.key" class="overview-stat">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </div>
        </div>

        <div class="workspace-content__body">
          <ArchiveDetailPanel
            :archive="archiveStore.selectedArchive"
            :on-append-timeline="handleAppendTimeline"
            :submitting-timeline="appendingTimeline"
          />
          <ArchiveForm :submitting="creating" :on-submit="handleCreateArchive" />
        </div>
      </section>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import ArchiveCard from "@/components/archive/ArchiveCard.vue";
import ArchiveDetailPanel from "@/components/archive/ArchiveDetailPanel.vue";
import ArchiveForm from "@/components/archive/ArchiveForm.vue";
import AppShell from "@/components/layout/AppShell.vue";
import { useArchiveStore } from "@/stores/archive";
import { useAuthStore } from "@/stores/auth";
import type { ArchiveCreatePayload, ArchiveTimelinePayload } from "@/types/api";

const auth = useAuthStore();
const archiveStore = useArchiveStore();
const router = useRouter();
const creating = ref(false);
const appendingTimeline = ref(false);

const overviewMetrics = computed(() => [
  { key: "totalArchives", label: "档案数", value: archiveStore.overview.totalArchives ?? 0 },
  { key: "activeCommunities", label: "社区数", value: archiveStore.overview.activeCommunities ?? 0 },
  { key: "totalTimelineEvents", label: "时间线事件", value: archiveStore.overview.totalTimelineEvents ?? 0 },
  { key: "totalMediaAssets", label: "媒体素材", value: archiveStore.overview.totalMediaAssets ?? 0 },
  { key: "totalSupportLinks", label: "支持关系", value: archiveStore.overview.totalSupportLinks ?? 0 },
]);

onMounted(async () => {
  try {
    if (!auth.currentUser && auth.token) {
      await auth.restoreSession();
    }

    if (!auth.token) {
      await router.push({ name: "login" });
      return;
    }

    await archiveStore.loadArchives();
    if (archiveStore.selectedId) {
      await archiveStore.selectArchive(archiveStore.selectedId);
    }
  } catch {
    auth.clearSession();
    await router.push({ name: "login" });
  }
});

async function handleCreateArchive(payload: ArchiveCreatePayload) {
  creating.value = true;
  try {
    await archiveStore.createArchive(payload);
  } finally {
    creating.value = false;
  }
}

async function handleAppendTimeline(payload: ArchiveTimelinePayload) {
  appendingTimeline.value = true;
  try {
    await archiveStore.appendTimeline(payload);
  } finally {
    appendingTimeline.value = false;
  }
}

async function logout() {
  auth.clearSession();
  await router.push({ name: "login" });
}
</script>