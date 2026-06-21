import api from '../lib/axios'
import type { LoginRequest, LoginResponse, RegisterRequest, SuccessResponse, User } from '../types'

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/api/v1/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    api.post<SuccessResponse>('/api/v1/auth/register', data).then((r) => r.data),

  me: () => api.get<User>('/api/v1/user/me').then((r) => r.data),
}
