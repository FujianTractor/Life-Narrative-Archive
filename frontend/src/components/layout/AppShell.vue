<template>
  <div class="app-shell">
    <header class="app-shell__header">
      <div>
        <p class="eyebrow">多模态生命叙事工作台</p>
        <h1>{{ title }}</h1>
      </div>
      <div class="app-shell__actions">
        <slot name="actions" />
      </div>
    </header>
    <main class="app-shell__content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
}>();
</script>
