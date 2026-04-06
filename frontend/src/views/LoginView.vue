<template>
  <div class="auth-view">
    <section class="auth-copy panel">
      <p class="eyebrow">生命叙事档案</p>
      <h1>先建立会话，再进入档案工作台</h1>
      <p>
        首轮迁移先打通登录、会话恢复、档案列表和详情查看。上传、多模态提取和 RAG 问答将在后续轮次继续接入。
      </p>
    </section>

    <section class="auth-form panel">
      <div class="auth-switch">
        <button :class="{ active: auth.authMode === 'login' }" type="button" @click="auth.authMode = 'login'">登录</button>
        <button :class="{ active: auth.authMode === 'register' }" type="button" @click="auth.authMode = 'register'">注册</button>
      </div>

      <form v-if="auth.authMode === 'login'" class="stack" @submit.prevent="submitLogin">
        <label>
          <span>用户名</span>
          <input v-model.trim="loginForm.username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
        </label>
        <button class="primary-button" type="submit" :disabled="auth.loading">{{ auth.loading ? '登录中...' : '登录' }}</button>
      </form>

      <form v-else class="stack" @submit.prevent="submitRegister">
        <label>
          <span>显示名称</span>
          <input v-model.trim="registerForm.displayName" autocomplete="nickname" />
        </label>
        <label>
          <span>用户名</span>
          <input v-model.trim="registerForm.username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="registerForm.password" type="password" autocomplete="new-password" required />
        </label>
        <button class="primary-button" type="submit" :disabled="auth.loading">{{ auth.loading ? '注册中...' : '注册并进入' }}</button>
      </form>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const errorMessage = ref("");

const loginForm = reactive({
  username: "",
  password: "",
});

const registerForm = reactive({
  displayName: "",
  username: "",
  password: "",
});

async function submitLogin() {
  errorMessage.value = "";
  try {
    await auth.loginWithPassword(loginForm);
    await router.push({ name: 'workspace' });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败';
  }
}

async function submitRegister() {
  errorMessage.value = "";
  try {
    await auth.registerWithPassword(registerForm);
    await router.push({ name: 'workspace' });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败';
  }
}
</script>
