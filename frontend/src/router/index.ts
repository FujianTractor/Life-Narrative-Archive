import { createRouter, createWebHistory } from "vue-router";

import LoginView from "@/views/LoginView.vue";
import ArchiveWorkspaceView from "@/views/ArchiveWorkspaceView.vue";

const routes = [
  {
    path: "/",
    redirect: "/workspace",
  },
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { public: true },
  },
  {
    path: "/workspace",
    name: "workspace",
    component: ArchiveWorkspaceView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const token = window.localStorage.getItem("lna.auth.token");
  if (!to.meta.public && !token) {
    return { name: "login" };
  }
  if (to.meta.public && token) {
    return { name: "workspace" };
  }
  return true;
});

export default router;
