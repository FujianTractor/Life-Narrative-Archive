<template>
  <section class="detail-panel">
    <template v-if="archive">
      <div class="detail-panel__hero">
        <div>
          <p class="eyebrow">档案详情</p>
          <h2>{{ archive.name }}</h2>
        </div>
        <p class="detail-panel__meta">最后更新 {{ formatUpdatedAt(archive.updatedAt) }}</p>
        <p>{{ archive.summary || "这份档案的摘要仍待补充，你可以先录入愿望和时间线。" }}</p>
      </div>

      <dl class="detail-grid">
        <div>
          <dt>年龄</dt>
          <dd>{{ archive.age }}</dd>
        </div>
        <div>
          <dt>社区</dt>
          <dd>{{ archive.community || "待补充" }}</dd>
        </div>
        <div>
          <dt>籍贯</dt>
          <dd>{{ archive.hometown || "待补充" }}</dd>
        </div>
        <div>
          <dt>角色</dt>
          <dd>{{ archive.role || "待补充" }}</dd>
        </div>
      </dl>

      <section class="detail-section">
        <h3>愿望</h3>
        <p>{{ archive.wish || "尚未整理愿望信息。" }}</p>
      </section>

      <section class="detail-section">
        <h3>标签</h3>
        <ul v-if="archive.tags?.length" class="detail-chip-list">
          <li v-for="tag in archive.tags" :key="tag" class="detail-chip">{{ tag }}</li>
        </ul>
        <p v-else class="detail-empty-copy">暂无标签。</p>
      </section>

      <section class="detail-section">
        <h3>支持者</h3>
        <ul v-if="archive.supporters?.length" class="detail-chip-list">
          <li v-for="supporter in archive.supporters" :key="supporter" class="detail-chip">{{ supporter }}</li>
        </ul>
        <p v-else class="detail-empty-copy">尚未补充支持者网络。</p>
      </section>

      <section class="detail-section">
        <h3>时间线</h3>
        <div v-if="archive.timeline?.length" class="timeline-list">
          <article v-for="entry in archive.timeline" :key="`${entry.year}-${entry.title}`" class="timeline-card">
            <p class="timeline-year">{{ entry.year }}</p>
            <h4>{{ entry.title }}</h4>
            <p>{{ entry.description }}</p>
          </article>
        </div>
        <p v-else class="detail-empty-copy">这份档案还没有时间线内容，现在可以直接补录。</p>
      </section>

      <section class="detail-section">
        <h3>媒体资产</h3>
        <ul v-if="hasAssets" class="detail-chip-list">
          <li v-if="imageCount" class="detail-chip">图片 {{ imageCount }} 张</li>
          <li v-if="videoCount" class="detail-chip">视频 {{ videoCount }} 个</li>
        </ul>
        <p v-else class="detail-empty-copy">当前还没有上传的图片或视频，后续轮次会继续接入。</p>
      </section>

      <section class="detail-section">
        <h3>补充时间线</h3>
        <p class="helper-text">保存后会直接刷新当前详情，不需要重新选择档案。</p>

        <form class="stack" @submit.prevent="handleSubmit">
          <label>
            <span>年份</span>
            <input v-model.trim="form.year" placeholder="如：1998" required />
          </label>

          <label>
            <span>标题</span>
            <input v-model.trim="form.title" placeholder="如：搬到社区居住" required />
          </label>

          <label>
            <span>描述</span>
            <textarea v-model.trim="form.description" rows="3" placeholder="补充这一阶段发生的事件或转折" required></textarea>
          </label>

          <button class="primary-button" type="submit" :disabled="submittingTimeline || !onAppendTimeline">
            {{ submittingTimeline ? "保存中..." : "添加时间线" }}
          </button>

          <p v-if="successMessage" class="success-text">{{ successMessage }}</p>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </form>
      </section>
    </template>
    <p v-else class="detail-empty">选择一份档案后，可在这里查看详情并补录时间线。</p>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";

import type { ArchiveDetail, ArchiveTimelinePayload } from "@/types/api";

const props = defineProps<{
  archive: ArchiveDetail | null;
  onAppendTimeline?: (payload: ArchiveTimelinePayload) => Promise<void>;
  submittingTimeline?: boolean;
}>();

const form = reactive({
  year: "",
  title: "",
  description: "",
});

const successMessage = ref("");
const errorMessage = ref("");

const imageCount = computed(() => props.archive?.assets?.images?.length ?? 0);
const videoCount = computed(() => props.archive?.assets?.videos?.length ?? 0);
const hasAssets = computed(() => imageCount.value > 0 || videoCount.value > 0);

function resetForm() {
  form.year = "";
  form.title = "";
  form.description = "";
}

function formatUpdatedAt(value?: string) {
  if (!value) {
    return "待补充";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

watch(
  () => props.archive?.id,
  () => {
    resetForm();
    successMessage.value = "";
    errorMessage.value = "";
  },
);

async function handleSubmit() {
  if (!props.archive || !props.onAppendTimeline) {
    return;
  }

  successMessage.value = "";
  errorMessage.value = "";

  try {
    await props.onAppendTimeline({
      year: form.year.trim(),
      title: form.title.trim(),
      description: form.description.trim(),
    });
    resetForm();
    successMessage.value = "时间线已追加到当前档案。";
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "追加时间线失败，请稍后重试";
  }
}
</script>