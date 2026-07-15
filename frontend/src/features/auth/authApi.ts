import { apiClient } from "../../services/apiClient";

export interface AuthResponse {
  userId: number;
  token: string;
}

export function register(email: string, password: string, fullName: string) {
  return apiClient
    .post<AuthResponse>("/auth/register", { email, password, fullName })
    .then((res) => res.data);
}

export function login(email: string, password: string) {
  return apiClient
    .post<AuthResponse>("/auth/login", { email, password })
    .then((res) => res.data);
}
