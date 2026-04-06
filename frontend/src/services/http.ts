import axios from "axios";

const errorMessageMap: Record<string, string> = {
  "Invalid username or password": "用户名或密码错误",
  "Username already exists": "用户名已存在",
  "Unauthorized or invalid token": "登录状态已失效，请重新登录",
  "Archive not found": "未找到对应档案",
  "Request validation failed": "请求参数校验失败",
  "Internal server error": "服务暂时不可用，请稍后重试",
};

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem("lna.auth.token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const rawMessage = error.response?.data?.message;
    if (typeof rawMessage === "string" && rawMessage.trim()) {
      return Promise.reject(new Error(errorMessageMap[rawMessage] ?? rawMessage));
    }

    if (error.response?.status === 401) {
      return Promise.reject(new Error("登录状态已失效，请重新登录"));
    }

    return Promise.reject(error);
  },
);

export default http;