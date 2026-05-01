<template>
  <button class="archive-card" :class="{ 'archive-card--active': active }" type="button">
    <span class="archive-card__title-row">
      <span class="archive-card__title">{{ archive.name }}</span>
      <span class="archive-card__age">{{ archive.age }} 岁</span>
    </span>
    <span class="archive-card__meta">{{ archive.community || archive.hometown || "待补充地域信息" }}</span>
    <span class="archive-card__summary">{{ archive.summary || archive.role || "暂无摘要" }}</span>
    <span v-if="archive.tags.length" class="archive-card__tags">{{ archive.tags.slice(0, 2).join(" · ") }}</span>
  </button>
</template>

<script setup lang="ts">
import type { ArchiveSummary } from "@/types/api";

defineProps<{
  archive: ArchiveSummary;
  active?: boolean;
}>();
</script>
