<template>
  <section class="archive-form">
    <div class="archive-form__header">
      <p class="eyebrow">{{ mode === "create" ? "新建档案" : "修改档案" }}</p>
      <h3>{{ mode === "create" ? "先录入基础资料，再继续补充故事线索" : "更新基础信息，再继续整理文档与影像" }}</h3>
      <p>{{ mode === "create" ? "摘要支持补充图片素材。创建完成后会自动切到档案列表并加载新档案。" : "这里负责修改基础字段与摘要图片，文档生成、时间线补录和影像上传可在下方继续处理。" }}</p>
    </div>

    <form class="archive-form__grid" @submit.prevent="handleSubmit">
      <label>
        <span>姓名</span>
        <input v-model.trim="form.name" required />
      </label>

      <label>
        <span>年龄</span>
        <input v-model.number="form.age" type="number" min="50" max="110" required />
      </label>

      <label>
        <span>社区</span>
        <input v-model.trim="form.community" />
      </label>

      <label>
        <span>籍贯</span>
        <input v-model.trim="form.hometown" />
      </label>

      <label>
        <span>角色</span>
        <input v-model.trim="form.role" placeholder="如：退休教师、社区手艺人、非遗讲述者" />
      </label>

      <label>
        <span>叙事色调</span>
        <select v-model="form.tone">
          <option value="amber">暖铜晨光</option>
          <option value="jade">松石纸本</option>
          <option value="rose">晚霞留影</option>
        </select>
      </label>

      <label class="field-span-2">
        <span>摘要</span>
        <textarea
          v-model.trim="form.summary"
          rows="4"
          placeholder="概括这份档案的生命经历、身份背景和当前关注点"
        />
      </label>

      <label class="field-span-2">
        <span>摘要图片</span>
        <input
          ref="imageInputRef"
          type="file"
          accept="image/*,.jpg,.jpeg,.png,.webp"
          multiple
          @change="handleImageChange"
        />
      </label>

      <div class="field-span-2 selected-files">
        <span v-if="summaryImages.length">{{ summaryImages.map((file) => file.name).join(" / ") }}</span>
        <span v-else>可上传人物照片、老照片或证件扫描件，创建后会进入影像资料板块。</span>
      </div>

      <label class="field-span-2">
        <span>标签</span>
        <input v-model.trim="form.tagsInput" placeholder="用逗号、顿号、分号或换行分隔，如：口述史、家庭记忆、社区融入" />
      </label>

      <div class="field-span-2 archive-form__footer">
        <button class="primary-button" type="submit" :disabled="submitting">
          {{ submitting ? (mode === "create" ? "创建中..." : "保存中...") : mode === "create" ? "创建档案" : "保存修改" }}
        </button>
        <p class="helper-text">{{ mode === "create" ? "创建后会自动进入档案列表并选中新档案。" : "保存基础资料后，当前档案会立即刷新。" }}</p>
      </div>

      <p v-if="errorMessage" class="error-text field-span-2">{{ errorMessage }}</p>
    </form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";

import type { ArchiveCreatePayload, ArchiveTone } from "@/types/api";

const props = withDefaults(
  defineProps<{
    onSubmit: (payload: ArchiveCreatePayload, summaryImages: File[]) => Promise<void>;
    submitting?: boolean;
    mode?: "create" | "edit";
    initialValue?: Partial<ArchiveCreatePayload>;
  }>(),
  {
    mode: "create",
  },
);

const emit = defineEmits<{
  submitted: [];
}>();

const form = reactive({
  name: "",
  age: 72,
  hometown: "",
  community: "",
  role: "",
  summary: "",
  tagsInput: "",
  tone: "amber" as ArchiveTone,
});

const errorMessage = ref("");
const summaryImages = ref<File[]>([]);
const imageInputRef = ref<HTMLInputElement | null>(null);

function parseList(value: string) {
  return value
    .split(/[，、；;,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function syncForm() {
  const value = props.initialValue;
  form.name = value?.name ?? "";
  form.age = value?.age ?? 72;
  form.hometown = value?.hometown ?? "";
  form.community = value?.community ?? "";
  form.role = value?.role ?? "";
  form.summary = value?.summary ?? "";
  form.tagsInput = value?.tags?.join("、") ?? "";
  form.tone = value?.tone ?? "amber";
  errorMessage.value = "";
  summaryImages.value = [];
  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }
}

function handleImageChange(event: Event) {
  const target = event.target as HTMLInputElement;
  summaryImages.value = Array.from(target.files ?? []);
}

watch(() => props.initialValue, syncForm, { immediate: true, deep: true });

async function handleSubmit() {
  errorMessage.value = "";

  try {
    await props.onSubmit(
      {
        name: form.name.trim(),
        age: form.age,
        hometown: form.hometown.trim(),
        community: form.community.trim(),
        role: form.role.trim(),
        summary: form.summary.trim(),
        wish: "",
        tags: parseList(form.tagsInput),
        supporters: [],
        tone: form.tone,
      },
      summaryImages.value,
    );
    emit("submitted");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : props.mode === "create" ? "创建档案失败，请稍后重试" : "保存修改失败，请稍后重试";
  }
}
</script>
