import http from "./http";
import type { AuthResponse } from "@/types/api";

export async function login(payload: { username: string; password: string }) {
  const { data } = await http.post<AuthResponse>("/auth/login", payload);
  return data;
}

export async function register(payload: { displayName?: string; username: string; password: string }) {
  const { data } = await http.post<AuthResponse>("/auth/register", payload);
  return data;
}

export async function fetchMe() {
  const { data } = await http.get<{ user: AuthResponse["user"] }>("/auth/me");
  return data;
}
