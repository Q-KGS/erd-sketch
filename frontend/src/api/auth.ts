import { apiClient } from './client'
import type { User, AuthTokens } from '@/models'

interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

interface LoginRequest {
  email: string
  password: string
}

interface AuthResponse {
  user: User
  tokens: AuthTokens
}

export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),

  refresh: (refreshToken: string) =>
    apiClient.post<AuthTokens>('/auth/refresh', { refreshToken }).then((r) => r.data),

  me: () =>
    apiClient.get<User>('/auth/me').then((r) => r.data),

  logout: () =>
    apiClient.post('/auth/logout').then((r) => r.data),
}
