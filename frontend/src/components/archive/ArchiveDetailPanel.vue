<template>
  <section class="detail-panel">
    <template v-if="archive">
      <header class="detail-panel__hero">
        <div>
          <p class="eyebrow">{{ editable ? "档案编辑" : "档案呈现" }}</p>
          <h2>{{ archive.name }}</h2>
          <p class="detail-panel__meta">最后更新：{{ formatUpdatedAt(archive.updatedAt) }}</p>
        </div>

        <div class="detail-hero__chips">
          <span class="section-badge">{{ archive.age }} 岁</span>
          <span class="section-badge">{{ archive.community || "待补充社区" }}</span>
          <span class="section-badge">{{ archive.role || "待补充角色" }}</span>
        </div>
      </header>

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
        <div class="section-head">
          <div>
            <h3>摘要</h3>
            <p class="helper-text">先看归纳后的生命叙事，再决定是否进入修改模式。</p>
          </div>
          <span class="section-badge">{{ editable ? "可上传文档" : "只读展示" }}</span>
        </div>
        <p class="detail-paragraph">{{ archive.summary || "当前还没有摘要内容。" }}</p>

        <div v-if="editable" class="upload-stack">
          <label>
            <span>摘要文档</span>
            <input
              ref="documentInputRef"
              type="file"
              accept=".doc,.docx,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              @change="handleDocumentChange"
            />
          </label>

          <div class="upload-row">
            <span class="helper-text">{{ documentFile?.name || "未选择文档" }}</span>
            <button
              class="primary-button"
              type="button"
              :disabled="!onGenerateSummaryFromDocument || generatingSummary || !documentFile"
              @click="handleGenerateSummary"
            >
              {{ generatingSummary ? "生成中..." : "上传文档并生成摘要时间线" }}
            </button>
          </div>

          <p v-if="summarySuccessMessage" class="success-text">{{ summarySuccessMessage }}</p>
          <p v-if="summaryErrorMessage" class="error-text">{{ summaryErrorMessage }}</p>
        </div>
      </section>

      <section class="detail-section">
        <h3>标签</h3>
        <ul v-if="archive.tags?.length" class="detail-chip-list">
          <li v-for="tag in archive.tags" :key="tag" class="detail-chip">{{ tag }}</li>
        </ul>
        <p v-else class="detail-empty-copy">暂无标签。</p>
      </section>

      <section class="detail-section">
        <div class="section-head">
          <div>
            <h3>时间线</h3>
            <p class="helper-text">在呈现模式下只阅读；进入编辑后才开放补录入口。</p>
          </div>
          <span class="section-badge">{{ archive.timeline?.length ?? 0 }} 条</span>
        </div>

        <div v-if="archive.timeline?.length" class="timeline-list">
          <article v-for="entry in archive.timeline" :key="`${entry.year}-${entry.title}`" class="timeline-card">
            <p class="timeline-year">{{ entry.year }}</p>
            <h4>{{ entry.title }}</h4>
            <p>{{ entry.description }}</p>
          </article>
        </div>
        <p v-else class="detail-empty-copy">这份档案还没有时间线内容。</p>

        <form v-if="editable" class="stack timeline-editor" @submit.prevent="handleAppendTimelineSubmit">
          <label>
            <span>年份</span>
            <input v-model.trim="timelineForm.year" placeholder="如：1998" required />
          </label>

          <label>
            <span>标题</span>
            <input v-model.trim="timelineForm.title" placeholder="如：搬到社区居住" required />
          </label>

          <label>
            <span>描述</span>
            <textarea
              v-model.trim="timelineForm.description"
              rows="3"
              placeholder="补充这一阶段发生的事件或转折"
              required
            />
          </label>

          <button class="secondary-button" type="submit" :disabled="submittingTimeline || !onAppendTimeline">
            {{ submittingTimeline ? "保存中..." : "补录时间线" }}
          </button>

          <p v-if="timelineSuccessMessage" class="success-text">{{ timelineSuccessMessage }}</p>
          <p v-if="timelineErrorMessage" class="error-text">{{ timelineErrorMessage }}</p>
        </form>
      </section>

      <section class="detail-section">
        <div class="section-head">
          <div>
            <h3>影像资料</h3>
            <p class="helper-text">统一呈现人物照片、老照片扫描件与人物视频。</p>
          </div>
          <span class="section-badge">{{ totalMediaCount }} 份</span>
        </div>

        <div v-if="editable" class="upload-stack">
          <label>
            <span>上传图片</span>
            <input
              ref="imageInputRef"
              type="file"
              accept="image/*,.jpg,.jpeg,.png,.webp"
              multiple
              @change="handleImageChange"
            />
          </label>

          <div class="upload-row">
            <span class="helper-text">{{ imageFiles.length ? imageFiles.map((file) => file.name).join(" / ") : "未选择图片" }}</span>
            <button
              class="secondary-button"
              type="button"
              :disabled="!onUploadImages || uploadingImages || !imageFiles.length"
              @click="handleUploadImages"
            >
              {{ uploadingImages ? "上传中..." : "上传图片素材" }}
            </button>
          </div>

          <label>
            <span>上传人物视频</span>
            <input
              ref="videoInputRef"
              type="file"
              accept="video/*,.mp4,.mov,.webm,.m4v"
              @change="handleVideoChange"
            />
          </label>

          <div class="upload-row">
            <span class="helper-text">{{ videoFile?.name || "未选择视频" }}</span>
            <button
              class="secondary-button"
              type="button"
              :disabled="!onUploadVideo || uploadingVideo || !videoFile"
              @click="handleUploadVideo"
            >
              {{ uploadingVideo ? "上传中..." : "上传人物视频" }}
            </button>
          </div>

          <p v-if="imageSuccessMessage" class="success-text">{{ imageSuccessMessage }}</p>
          <p v-if="imageErrorMessage" class="error-text">{{ imageErrorMessage }}</p>
          <p v-if="videoSuccessMessage" class="success-text">{{ videoSuccessMessage }}</p>
          <p v-if="videoErrorMessage" class="error-text">{{ videoErrorMessage }}</p>
        </div>

        <div v-if="archive.assets?.images?.length" class="image-grid">
          <article v-for="image in archive.assets.images" :key="`${image.name}-${image.url}`" class="image-card">
            <img class="image-preview" :src="resolveAssetUrl(image.url)" :alt="image.name" loading="lazy" />
            <strong>{{ image.name }}</strong>
          </article>
        </div>

        <div v-if="archive.assets?.videos?.length" class="media-list">
          <article v-for="video in archive.assets.videos" :key="`${video.name}-${video.url}`" class="video-card">
            <strong>{{ video.name }}</strong>
            <video v-if="video.url" class="video-preview" controls preload="metadata" :src="resolveAssetUrl(video.url)" />
            <a v-if="video.url" class="helper-text" :href="resolveAssetUrl(video.url)" target="_blank" rel="noreferrer">
              打开原视频
            </a>
          </article>
        </div>

        <p v-if="!totalMediaCount" class="detail-empty-copy">当前还没有影像资料。</p>
      </section>

      <div v-if="showEditAction && !editable" class="detail-panel__footer">
        <p class="helper-text">如需修改基础信息、补录时间线、生成摘要或上传新影像，请滑到底部后进入修改模式。</p>
        <button class="primary-button" type="button" @click="emit('edit')">{{ editActionLabel }}</button>
      </div>
    </template>

    <p v-else class="detail-empty">从左侧列表选择一份档案后，这里会显示人物摘要、时间线和影像资料。</p>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";

import type { ArchiveDetail, ArchiveTimelinePayload } from "@/types/api";

const props = withDefaults(
  defineProps<{
    archive: ArchiveDetail | null;
    editable?: boolean;
    showEditAction?: boolean;
    editActionLabel?: string;
    onAppendTimeline?: (payload: ArchiveTimelinePayload) => Promise<void>;
    submittingTimeline?: boolean;
    onGenerateSummaryFromDocument?: (file: File) => Promise<void>;
    generatingSummary?: boolean;
    onUploadImages?: (files: File[]) => Promise<void>;
    uploadingImages?: boolean;
    onUploadVideo?: (file: File) => Promise<void>;
    uploadingVideo?: boolean;
  }>(),
  {
    editable: false,
    showEditAction: false,
    editActionLabel: "修改档案",
    submittingTimeline: false,
    generatingSummary: false,
    uploadingImages: false,
    uploadingVideo: false,
  },
);

const emit = defineEmits<{
  edit: [];
}>();

const timelineForm = reactive({
  year: "",
  title: "",
  description: "",
});

const documentFile = ref<File | null>(null);
const imageFiles = ref<File[]>([]);
const videoFile = ref<File | null>(null);
const documentInputRef = ref<HTMLInputElement | null>(null);
const imageInputRef = ref<HTMLInputElement | null>(null);
const videoInputRef = ref<HTMLInputElement | null>(null);

const timelineSuccessMessage = ref("");
const timelineErrorMessage = ref("");
const summarySuccessMessage = ref("");
const summaryErrorMessage = ref("");
const imageSuccessMessage = ref("");
const imageErrorMessage = ref("");
const videoSuccessMessage = ref("");
const videoErrorMessage = ref("");

const totalMediaCount = computed(() => (props.archive?.assets?.images?.length ?? 0) + (props.archive?.assets?.videos?.length ?? 0));
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
const assetBaseUrl = resolveAssetBaseUrl();

function resetTimelineForm() {
  timelineForm.year = "";
  timelineForm.title = "";
  timelineForm.description = "";
}

function resetUploadState() {
  documentFile.value = null;
  imageFiles.value = [];
  videoFile.value = null;
  if (documentInputRef.value) {
    documentInputRef.value.value = "";
  }
  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }
  if (videoInputRef.value) {
    videoInputRef.value.value = "";
  }
}

function clearMessages() {
  timelineSuccessMessage.value = "";
  timelineErrorMessage.value = "";
  summarySuccessMessage.value = "";
  summaryErrorMessage.value = "";
  imageSuccessMessage.value = "";
  imageErrorMessage.value = "";
  videoSuccessMessage.value = "";
  videoErrorMessage.value = "";
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

function handleDocumentChange(event: Event) {
  const target = event.target as HTMLInputElement;
  documentFile.value = target.files?.[0] ?? null;
  summarySuccessMessage.value = "";
  summaryErrorMessage.value = "";
}

function handleImageChange(event: Event) {
  const target = event.target as HTMLInputElement;
  imageFiles.value = Array.from(target.files ?? []);
  imageSuccessMessage.value = "";
  imageErrorMessage.value = "";
}

function handleVideoChange(event: Event) {
  const target = event.target as HTMLInputElement;
  videoFile.value = target.files?.[0] ?? null;
  videoSuccessMessage.value = "";
  videoErrorMessage.value = "";
}

function resolveAssetBaseUrl() {
  const configuredBaseUrl = (import.meta.env.VITE_ASSET_BASE_URL as string | undefined)?.trim();
  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/+$/, "");
  }

  if (!apiBaseUrl) {
    return "";
  }

  try {
    return new URL(apiBaseUrl).origin;
  } catch {
    return "";
  }
}

function resolveAssetUrl(url?: string) {
  if (!url) {
    return "";
  }

  if (/^https?:\/\//i.test(url) || url.startsWith("blob:") || url.startsWith("data:")) {
    return url;
  }

  if (!assetBaseUrl) {
    return url;
  }

  if (url.startsWith("/")) {
    return `${assetBaseUrl}${url}`;
  }

  return `${assetBaseUrl}/${url}`;
}

watch(
  () => props.archive?.id,
  () => {
    resetTimelineForm();
    resetUploadState();
    clearMessages();
  },
);

async function handleAppendTimelineSubmit() {
  if (!props.archive || !props.onAppendTimeline) {
    return;
  }

  timelineSuccessMessage.value = "";
  timelineErrorMessage.value = "";

  try {
    await props.onAppendTimeline({
      year: timelineForm.year.trim(),
      title: timelineForm.title.trim(),
      description: timelineForm.description.trim(),
    });
    resetTimelineForm();
    timelineSuccessMessage.value = "时间线已追加到当前档案。";
  } catch (error) {
    timelineErrorMessage.value = error instanceof Error ? error.message : "补录时间线失败，请稍后重试";
  }
}

async function handleGenerateSummary() {
  if (!props.onGenerateSummaryFromDocument || !documentFile.value) {
    return;
  }

  summarySuccessMessage.value = "";
  summaryErrorMessage.value = "";

  try {
    await props.onGenerateSummaryFromDocument(documentFile.value);
    summarySuccessMessage.value = "文档已上传，摘要和时间线已更新。";
    documentFile.value = null;
    if (documentInputRef.value) {
      documentInputRef.value.value = "";
    }
  } catch (error) {
    summaryErrorMessage.value = error instanceof Error ? error.message : "生成摘要失败，请稍后重试";
  }
}

async function handleUploadImages() {
  if (!props.onUploadImages || !imageFiles.value.length) {
    return;
  }

  imageSuccessMessage.value = "";
  imageErrorMessage.value = "";

  try {
    await props.onUploadImages(imageFiles.value);
    imageSuccessMessage.value = "图片素材已加入影像资料板块。";
    imageFiles.value = [];
    if (imageInputRef.value) {
      imageInputRef.value.value = "";
    }
  } catch (error) {
    imageErrorMessage.value = error instanceof Error ? error.message : "上传图片失败，请稍后重试";
  }
}

async function handleUploadVideo() {
  if (!props.onUploadVideo || !videoFile.value) {
    return;
  }

  videoSuccessMessage.value = "";
  videoErrorMessage.value = "";

  try {
    await props.onUploadVideo(videoFile.value);
    videoSuccessMessage.value = "人物视频已上传到当前档案。";
    videoFile.value = null;
    if (videoInputRef.value) {
      videoInputRef.value.value = "";
    }
  } catch (error) {
    videoErrorMessage.value = error instanceof Error ? error.message : "上传视频失败，请稍后重试";
  }
}
</script>
