// ── Auth ──────────────────────────────────────────────
export interface User {
  id: number
  username: string
  email: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
}

export interface LoginResponse {
  user: User
  accessToken: string
  refreshToken: string
}

export interface SuccessResponse {
  status: boolean
  message: string
}

// ── Workflow ──────────────────────────────────────────
export type WorkflowStatus = 'ACTIVE' | 'INACTIVE' | 'DRAFT'
export type TriggerType = 'WEBHOOK' | 'SCHEDULE' | 'MANUAL'
export type ActionType =
  | 'HTTP_REQUEST'
  | 'EMAIL'
  | 'SLACK_WEBHOOK'
  | 'DATABASE_INSERT'
  | 'DELAY'
  | 'CONDITION'

export interface WorkflowStepResponse {
  id: number
  stepOrder: number
  actionType: ActionType
  actionConfig: string
  maxRetries: number
}

export interface WorkflowResponse {
  id: number
  name: string
  description: string
  userId: number
  status: WorkflowStatus
  triggerType: TriggerType
  triggerConfig: string
  steps: WorkflowStepResponse[]
  createdAt: string
  updatedAt: string
}

export interface CreateWorkflowRequest {
  name: string
  description?: string
  triggerType: TriggerType
  triggerConfig?: string
  steps?: AddStepRequest[]
}

export interface UpdateWorkflowRequest {
  name?: string
  description?: string
}

export interface AddStepRequest {
  stepOrder: number
  actionType: ActionType
  actionConfig?: string
  maxRetries?: number
}

// ── Execution ─────────────────────────────────────────
export type ExecutionStatus = 'QUEUED' | 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
export type StepStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export interface ExecutionStepResponse {
  id: number
  stepOrder: number
  actionType: ActionType
  status: StepStatus
  result: string
  errorMessage: string
  executedAt: string
}

export interface ExecutionResponse {
  id: number
  workflowId: number
  userId: number
  status: ExecutionStatus
  triggerType: string
  startedAt: string
  completedAt: string
  errorMessage: string
  createdAt: string
  steps: ExecutionStepResponse[]
}

export interface AnalyticsSummaryResponse {
  totalExecutions: number
  completedExecutions: number
  failedExecutions: number
  runningExecutions: number
  successRate: number
  dlqCount: number
}

export interface DeadLetterResponse {
  id: number
  executionId: number
  workflowId: number
  stepOrder: number
  actionType: ActionType
  payload: string
  failureReason: string
  retryCount: number
  replayed: boolean
  createdAt: string
}
