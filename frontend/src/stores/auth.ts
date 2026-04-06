import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchMe, login, register } from "@/services/authService";
import type { UserProfile } from "@/types/api";

const TOKEN_KEY = "lna.auth.token";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(window.localStorage.getItem(TOKEN_KEY) ?? "");
  const currentUser = ref<UserProfile | null>(null);
  const authMode = ref<"login" | "register">("login");
  const loading = ref(false);

  function setSession(accessToken: string, user: UserProfile) {
    token.value = accessToken;
    currentUser.value = user;
    window.localStorage.setItem(TOKEN_KEY, accessToken);
  }

  function clearSession() {
    token.value = "";
    currentUser.value = null;
    window.localStorage.removeItem(TOKEN_KEY);
  }

  async function loginWithPassword(payload: { username: string; password: string }) {
    loading.value = true;
    try {
      const data = await login(payload);
      setSession(data.accessToken, data.user);
    } finally {
      loading.value = false;
    }
  }

  async function registerWithPassword(payload: { displayName?: string; username: string; password: string }) {
    loading.value = true;
    try {
      const data = await register(payload);
      setSession(data.accessToken, data.user);
    } finally {
      loading.value = false;
    }
  }

  async function restoreSession() {
    if (!token.value) {
      return;
    }
    loading.value = true;
    try {
      const data = await fetchMe();
      currentUser.value = data.user;
    } catch (error) {
      clearSession();
      throw error;
    } finally {
      loading.value = false;
    }
  }

  return {
    token,
    currentUser,
    authMode,
    loading,
    isAuthenticated: computed(() => Boolean(token.value)),
    setSession,
    clearSession,
    loginWithPassword,
    registerWithPassword,
    restoreSession,
  };
});
