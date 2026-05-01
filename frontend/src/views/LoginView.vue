<template>
  <div class="auth-scene" :style="{ '--auth-cover': `url(${loginCoverUrl})` }">
    <div class="auth-scene__scrim" />

    <section class="auth-modal panel">
      <p class="eyebrow">生命叙事档案</p>
      <h1>生命叙事档案</h1>
     

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
        <button class="primary-button" type="submit" :disabled="auth.loading">{{ auth.loading ? "登录中..." : "登录" }}</button>
      </form>

      <form v-else class="stack" @submit.prevent="submitRegister">
        <label>
          <span>用户名</span>
          <input v-model.trim="registerForm.username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="registerForm.password" type="password" autocomplete="new-password" required />
        </label>
        <button class="primary-button" type="submit" :disabled="auth.loading">{{ auth.loading ? "注册中..." : "注册并进入" }}</button>
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
const loginCoverUrl = new URL("../../img/load.jpg", import.meta.url).href;

const loginForm = reactive({
  username: "",
  password: "",
});

const registerForm = reactive({
  username: "",
  password: "",
});

async function submitLogin() {
  errorMessage.value = "";
  try {
    await auth.loginWithPassword(loginForm);
    await router.push({ name: "workspace" });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
  }
}

async function submitRegister() {
  errorMessage.value = "";
  try {
    await auth.registerWithPassword({
      username: registerForm.username.trim(),
      password: registerForm.password,
      displayName: registerForm.username.trim(),
    });
    await router.push({ name: "workspace" });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "注册失败";
  }
}
</script>
