import api from '../lib/axios'
import type {
  WorkflowResponse,
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  AddStepRequest,
  WorkflowStepResponse,
} from '../types'

interface SuccessResponse { success: boolean; message: string }

export const workflowsApi = {
  getAll: () =>
    api.get<WorkflowResponse[]>('/api/v1/workflows').then((r) => r.data),

  getById: (id: number) =>
    api.get<WorkflowResponse>(`/api/v1/workflows/${id}`).then((r) => r.data),

  create: (data: CreateWorkflowRequest) =>
    api.post<WorkflowResponse>('/api/v1/workflows', data).then((r) => r.data),

  update: (id: number, data: UpdateWorkflowRequest) =>
    api.put<SuccessResponse>(`/api/v1/workflows/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    api.delete<SuccessResponse>(`/api/v1/workflows/${id}`).then((r) => r.data),

  activate: (id: number) =>
    api.post<WorkflowResponse>(`/api/v1/workflows/${id}/activate`).then((r) => r.data),

  deactivate: (id: number) =>
    api.post<WorkflowResponse>(`/api/v1/workflows/${id}/deactivate`).then((r) => r.data),

  trigger: (id: number) =>
    api.post<SuccessResponse>(`/api/v1/workflows/${id}/trigger`).then((r) => r.data),

  addStep: (id: number, data: AddStepRequest) =>
    api.post<WorkflowStepResponse>(`/api/v1/workflows/${id}/steps`, data).then((r) => r.data),
}
