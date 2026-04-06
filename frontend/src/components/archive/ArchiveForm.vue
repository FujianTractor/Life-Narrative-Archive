<template>
  <section class="panel archive-form">
    <div class="archive-form__header">
      <p class="eyebrow">新建档案</p>
      <h3>先录核心资料，再补故事脉络</h3>
      <p>本轮先收集基础身份、摘要、愿望和支持关系，上传素材与多模态解析将在后续迭代接入。</p>
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
        <input v-model.trim="form.role" placeholder="如：退休教师、社区手艺人" />
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
        <textarea v-model.trim="form.summary" rows="3" placeholder="概括这份档案的生命经历、身份背景和当前关注点"></textarea>
      </label>

      <label class="field-span-2">
        <span>愿望</span>
        <textarea v-model.trim="form.wish" rows="3" placeholder="记录她当前希望完成、保留或传递的事情"></textarea>
      </label>

      <label class="field-span-2">
        <span>标签</span>
        <input v-model.trim="form.tagsInput" placeholder="用逗号分隔，如：口述史、家庭记忆、数字融入" />
      </label>

      <label class="field-span-2">
        <span>支持者</span>
        <input v-model.trim="form.supportersInput" placeholder="用逗号分隔，如：社区社工、女儿、志愿者" />
      </label>

      <div class="field-span-2 archive-form__footer">
        <button class="primary-button" type="submit" :disabled="submitting">
          {{ submitting ? "创建中..." : "创建档案" }}
        </button>
        <p class="helper-text">创建成功后会自动刷新左侧列表，并选中这份新档案。</p>
      </div>

      <p v-if="successMessage" class="success-text field-span-2">{{ successMessage }}</p>
      <p v-if="errorMessage" class="error-text field-span-2">{{ errorMessage }}</p>
    </form>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";

import type { ArchiveCreatePayload, ArchiveTone } from "@/types/api";

const props = defineProps<{
  onSubmit: (payload: ArchiveCreatePayload) => Promise<void>;
  submitting?: boolean;
}>();

const form = reactive({
  name: "",
  age: 72,
  hometown: "",
  community: "",
  role: "",
  summary: "",
  wish: "",
  tagsInput: "",
  supportersInput: "",
  tone: "amber" as ArchiveTone,
});

const successMessage = ref("");
const errorMessage = ref("");

function parseList(value: string) {
  return value
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function resetForm() {
  form.name = "";
  form.age = 72;
  form.hometown = "";
  form.community = "";
  form.role = "";
  form.summary = "";
  form.wish = "";
  form.tagsInput = "";
  form.supportersInput = "";
  form.tone = "amber";
}

async function handleSubmit() {
  successMessage.value = "";
  errorMessage.value = "";

  try {
    await props.onSubmit({
      name: form.name.trim(),
      age: form.age,
      hometown: form.hometown.trim(),
      community: form.community.trim(),
      role: form.role.trim(),
      summary: form.summary.trim(),
      wish: form.wish.trim(),
      tags: parseList(form.tagsInput),
      supporters: parseList(form.supportersInput),
      tone: form.tone,
    });
    resetForm();
    successMessage.value = "档案已创建并载入工作台。";
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "创建档案失败，请稍后重试";
  }
}
</script>