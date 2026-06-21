import api from '../lib/axios'
import type {
  ExecutionResponse,
  ExecutionStatus,
  AnalyticsSummaryResponse,
  DeadLetterResponse,
} from '../types'

export const executionsApi = {
  getMyExecutions: () =>
    api.get<ExecutionResponse[]>('/api/v1/executions').then((r) => r.data),

  getById: (id: number) =>
    api.get<ExecutionResponse>(`/api/v1/executions/${id}`).then((r) => r.data),

  getByWorkflow: (workflowId: number) =>
    api.get<ExecutionResponse[]>(`/api/v1/executions/workflow/${workflowId}`).then((r) => r.data),

  filterByStatus: (status: ExecutionStatus) =>
    api.get<ExecutionResponse[]>(`/api/v1/executions/filter?status=${status}`).then((r) => r.data),

  retry: (executionId: number) =>
    api.post<ExecutionResponse>(`/api/v1/executions/${executionId}/retry`).then((r) => r.data),

  getAnalytics: () =>
    api.get<AnalyticsSummaryResponse>('/api/v1/executions/analytics/summary').then((r) => r.data),

  getDLQ: () =>
    api.get<DeadLetterResponse[]>('/api/v1/executions/dlq').then((r) => r.data),
}
