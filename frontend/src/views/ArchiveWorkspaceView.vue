<template>
  <AppShell title="生命叙事档案">
    <template #actions>
      <div class="shell-user">
        <span>{{ auth.currentUser?.displayName || auth.currentUser?.username || "未登录" }}</span>
        <button class="secondary-button" type="button" @click="logout">退出登录</button>
      </div>
    </template>

    <section class="workspace-frame">
      <aside class="workspace-nav panel">
        <div class="workspace-nav__brand">
          <p class="eyebrow">Life Narrative Archive</p>
          <h2>记忆工作台</h2>
          <p>左侧切换看板、档案列表与关于我们；编辑入口统一收在档案末端。</p>
        </div>

        <nav class="workspace-nav__menu" aria-label="工作台导航">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="workspace-nav__link"
            :class="{ 'workspace-nav__link--active': activeView === item.key }"
            type="button"
            @click="switchView(item.key)"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.description }}</small>
          </button>
        </nav>

        <div class="workspace-nav__footer">
          <p class="helper-text">当前共整理 {{ archiveStore.overview.totalArchives ?? 0 }} 份档案，影像素材 {{ archiveStore.overview.totalMediaAssets ?? 0 }} 份。</p>
        </div>
      </aside>

      <section class="workspace-main">
        <template v-if="activeView === 'dashboard'">
          <section class="dashboard-hero panel">
            <div class="dashboard-hero__copy">
              <p class="eyebrow">档案看板</p>
              <h2>先看全局，再决定今天要补哪一段生命故事。</h2>
              <p>
                看板保留整体进度、人物聚焦和建档入口。向下滑动到末端后，可以弹出新建档案卡片；创建后会自动进入档案列表。
              </p>

              <div class="dashboard-hero__actions">
                <button class="primary-button" type="button" @click="openCreateModal">立即建档</button>
                <button class="secondary-button" type="button" @click="switchView('archives')">查看档案列表</button>
              </div>
            </div>

            <div class="dashboard-hero__spotlight">
              <p class="eyebrow">当前聚焦</p>
              <template v-if="spotlightArchive">
                <h3>{{ spotlightArchive.name }}</h3>
                <p>{{ spotlightArchive.summary || "这份档案正在等待摘要补充。" }}</p>
                <button class="secondary-button" type="button" @click="openArchiveFromDashboard(spotlightArchive.id)">进入档案</button>
              </template>
              <template v-else>
                <h3>还没有档案</h3>
                <p>先创建第一份档案，工作台会自动生成后续的查看与编辑路径。</p>
              </template>
            </div>
          </section>

          <section class="overview-grid">
            <article v-for="metric in overviewMetrics" :key="metric.key" class="overview-stat panel">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </section>

          <section class="dashboard-story panel">
            <div class="section-head">
              <div>
                <p class="eyebrow">时间线预览</p>
                <h3>最近正在阅读的人物节点</h3>
              </div>
              <span class="section-badge">{{ spotlightTimeline.length }} 条</span>
            </div>

            <div v-if="spotlightTimeline.length" class="timeline-list">
              <article v-for="entry in spotlightTimeline" :key="`${entry.year}-${entry.title}`" class="timeline-card">
                <p class="timeline-year">{{ entry.year }}</p>
                <h4>{{ entry.title }}</h4>
                <p>{{ entry.description }}</p>
              </article>
            </div>
            <p v-else class="detail-empty-copy">当前没有可展示的时间线内容，上传文档后会自动生成。</p>
          </section>

          <section class="dashboard-cta panel">
            <p class="eyebrow">看板末端</p>
            <h3>滑动到这里后，就可以发起一份新的档案。</h3>
            <p>新建卡片会以弹窗形式出现，避免在看板浏览过程中直接暴露编辑表单。</p>
            <button class="primary-button" type="button" @click="openCreateModal">弹出新建档案卡片</button>
          </section>
        </template>

        <template v-else-if="activeView === 'archives'">
          <section class="archive-browser">
            <aside class="archive-browser__list panel">
              <div class="archive-browser__header">
                <div>
                  <p class="eyebrow">档案列表</p>
                  <h2>点击档案进入只读呈现</h2>
                </div>
                <input
                  v-model.trim="archiveStore.query"
                  class="search-input"
                  placeholder="搜索姓名、社区、角色、摘要或标签"
                />
              </div>

              <div v-if="archiveStore.filteredArchives.length" class="archive-list">
                <ArchiveCard
                  v-for="archive in archiveStore.filteredArchives"
                  :key="archive.id"
                  :archive="archive"
                  :active="archive.id === archiveStore.selectedId"
                  @click="handleSelectArchive(archive.id)"
                />
              </div>

              <p v-else class="archive-list__empty">
                {{ archiveStore.loading ? "正在加载档案..." : "没有匹配的档案，请调整搜索条件。" }}
              </p>
            </aside>

            <section class="archive-browser__detail panel">
              <ArchiveDetailPanel
                :archive="archiveStore.selectedArchive"
                :show-edit-action="Boolean(archiveStore.selectedArchive)"
                @edit="openEditModal"
              />
            </section>
          </section>
        </template>

        <template v-else>
          <section class="about-view panel">
            <div class="section-head">
              <div>
                <p class="eyebrow">关于我们</p>
                <h2>为社区人物建立可持续补充的生命档案</h2>
              </div>
              <span class="section-badge">多模态采集</span>
            </div>

            <div class="about-grid">
              <article class="about-block">
                <h3>我们在做什么</h3>
                <p>把人物基础信息、摘要、时间线、老照片与人物视频收束到同一份档案里，方便后续继续追补与整理。</p>
              </article>
              <article class="about-block">
                <h3>为什么改成这种布局</h3>
                <p>浏览态与编辑态拆开以后，档案列表先负责呈现；滑动到末端才进入修改，能减少误操作，也更接近真实采编流程。</p>
              </article>
              <article class="about-block">
                <h3>接下来可以继续补什么</h3>
                <p>当前已经支持文档生成摘要时间线、摘要图片上传、人物视频上传。后续还可以继续接 OCR、多模态理解和 RAG 问答。</p>
              </article>
            </div>
          </section>
        </template>
      </section>
    </section>
  </AppShell>

  <Teleport to="body">
    <div v-if="createModalOpen" class="modal-backdrop" @click.self="closeCreateModal">
      <section class="modal-card panel">
        <div class="modal-card__header">
          <div>
            <p class="eyebrow">弹窗建档</p>
            <h2>新建档案</h2>
          </div>
          <button class="modal-close" type="button" @click="closeCreateModal">关闭</button>
        </div>

        <ArchiveForm
          :submitting="creating"
          mode="create"
          :on-submit="handleCreateArchive"
          @submitted="handleCreateSubmitted"
        />
      </section>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="editModalOpen && archiveStore.selectedArchive" class="modal-backdrop" @click.self="closeEditModal">
      <section class="modal-card modal-card--wide panel">
        <div class="modal-card__header">
          <div>
            <p class="eyebrow">末端修改入口</p>
            <h2>修改档案</h2>
          </div>
          <button class="modal-close" type="button" @click="closeEditModal">关闭</button>
        </div>

        <div class="editor-stack">
          <ArchiveForm
            :submitting="updating"
            mode="edit"
            :initial-value="editInitialValue"
            :on-submit="handleUpdateArchive"
            @submitted="handleUpdateSubmitted"
          />

          <ArchiveDetailPanel
            :archive="archiveStore.selectedArchive"
            editable
            :on-append-timeline="handleAppendTimeline"
            :submitting-timeline="appendingTimeline"
            :on-generate-summary-from-document="handleGenerateSummaryFromDocument"
            :generating-summary="generatingSummary"
            :on-upload-images="handleUploadImages"
            :uploading-images="uploadingImages"
            :on-upload-video="handleUploadVideo"
            :uploading-video="uploadingVideo"
          />
        </div>
      </section>
    </div>
  </Teleport>
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
import type { ArchiveCreatePayload, ArchiveTimelinePayload, ArchiveTone } from "@/types/api";

type WorkspaceView = "dashboard" | "archives" | "about";

const auth = useAuthStore();
const archiveStore = useArchiveStore();
const router = useRouter();

const activeView = ref<WorkspaceView>("dashboard");
const createModalOpen = ref(false);
const editModalOpen = ref(false);
const creating = ref(false);
const updating = ref(false);
const appendingTimeline = ref(false);
const generatingSummary = ref(false);
const uploadingImages = ref(false);
const uploadingVideo = ref(false);

const navItems: Array<{ key: WorkspaceView; label: string; description: string }> = [
  { key: "dashboard", label: "档案看板", description: "总览进度与建档入口" },
  { key: "archives", label: "档案列表", description: "查看档案并在末端进入修改" },
  { key: "about", label: "关于我们", description: "查看项目定位与后续方向" },
];

function normalizeTone(value?: string): ArchiveTone {
  return value === "jade" || value === "rose" ? value : "amber";
}

const overviewMetrics = computed(() => [
  { key: "totalArchives", label: "档案数", value: archiveStore.overview.totalArchives ?? 0 },
  { key: "activeCommunities", label: "社区数", value: archiveStore.overview.activeCommunities ?? 0 },
  { key: "totalTimelineEvents", label: "时间线事件", value: archiveStore.overview.totalTimelineEvents ?? 0 },
  { key: "totalMediaAssets", label: "影像素材", value: archiveStore.overview.totalMediaAssets ?? 0 },
]);

const spotlightArchive = computed(() => archiveStore.selectedArchive ?? archiveStore.filteredArchives[0] ?? archiveStore.archives[0] ?? null);
const spotlightTimeline = computed(() => archiveStore.selectedArchive?.timeline?.slice(0, 3) ?? []);

const editInitialValue = computed(() => {
  const archive = archiveStore.selectedArchive;
  if (!archive) {
    return undefined;
  }
  return {
    name: archive.name,
    age: archive.age,
    hometown: archive.hometown,
    community: archive.community,
    role: archive.role,
    summary: archive.summary,
    tags: archive.tags,
    tone: normalizeTone(archive.tone),
  };
});

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

function switchView(view: WorkspaceView) {
  activeView.value = view;
}

function openCreateModal() {
  createModalOpen.value = true;
}

function closeCreateModal() {
  createModalOpen.value = false;
}

function openEditModal() {
  editModalOpen.value = true;
}

function closeEditModal() {
  editModalOpen.value = false;
}

async function handleSelectArchive(archiveId: string) {
  activeView.value = "archives";
  await archiveStore.selectArchive(archiveId);
}

async function openArchiveFromDashboard(archiveId: string) {
  await handleSelectArchive(archiveId);
}

async function handleCreateArchive(payload: ArchiveCreatePayload, summaryImages: File[]) {
  creating.value = true;
  try {
    await archiveStore.createArchive(payload);
    if (summaryImages.length) {
      await archiveStore.uploadImages(summaryImages);
    }
  } finally {
    creating.value = false;
  }
}

function handleCreateSubmitted() {
  closeCreateModal();
  activeView.value = "archives";
}

async function handleUpdateArchive(payload: ArchiveCreatePayload, summaryImages: File[]) {
  updating.value = true;
  try {
    await archiveStore.updateArchive(payload);
    if (summaryImages.length) {
      await archiveStore.uploadImages(summaryImages);
    }
  } finally {
    updating.value = false;
  }
}

function handleUpdateSubmitted() {
  closeEditModal();
}

async function handleAppendTimeline(payload: ArchiveTimelinePayload) {
  appendingTimeline.value = true;
  try {
    await archiveStore.appendTimeline(payload);
  } finally {
    appendingTimeline.value = false;
  }
}

async function handleGenerateSummaryFromDocument(file: File) {
  generatingSummary.value = true;
  try {
    await archiveStore.generateSummaryFromDocument(file);
  } finally {
    generatingSummary.value = false;
  }
}

async function handleUploadImages(files: File[]) {
  uploadingImages.value = true;
  try {
    await archiveStore.uploadImages(files);
  } finally {
    uploadingImages.value = false;
  }
}

async function handleUploadVideo(file: File) {
  uploadingVideo.value = true;
  try {
    await archiveStore.uploadVideo(file);
  } finally {
    uploadingVideo.value = false;
  }
}

async function logout() {
  auth.clearSession();
  await router.push({ name: "login" });
}
</script>
