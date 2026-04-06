<template>
  <button class="archive-card" :class="{ 'archive-card--active': active }" type="button">
    <span class="archive-card__title">{{ archive.name }}</span>
    <span class="archive-card__meta">{{ archive.community || archive.hometown || "待补充社区信息" }}</span>
    <span class="archive-card__summary">{{ archive.summary || archive.wish || "暂无摘要" }}</span>
  </button>
</template>

<script setup lang="ts">
import type { ArchiveSummary } from "@/types/api";

defineProps<{
  archive: ArchiveSummary;
  active?: boolean;
}>();
</script>
