import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workflowsApi } from '../api/workflows'
import { executionsApi } from '../api/executions'
import type { ExecutionStatus, WorkflowStatus, TriggerType } from '../types'

const statusColors: Record<WorkflowStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  INACTIVE: 'bg-slate-100 text-slate-600',
  DRAFT: 'bg-amber-100 text-amber-700',
}

const execStatusColors: Record<ExecutionStatus, string> = {
  QUEUED: 'bg-amber-100 text-amber-700',
  PENDING: 'bg-amber-100 text-amber-700',
  RUNNING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

const triggerLabels: Record<TriggerType, string> = {
  WEBHOOK: 'Webhook',
  SCHEDULE: 'Schedule (CRON)',
  MANUAL: 'Manual',
}

export default function WorkflowDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const wfId = Number(id)

  const { data: workflow, isLoading: wfLoading } = useQuery({
    queryKey: ['workflow', wfId],
    queryFn: () => workflowsApi.getById(wfId),
    enabled: !!wfId,
  })

  const { data: executions, isLoading: exLoading } = useQuery({
    queryKey: ['executions-workflow', wfId],
    queryFn: () => executionsApi.getByWorkflow(wfId),
    enabled: !!wfId,
    refetchInterval: 10_000,
  })

  const activateMutation = useMutation({
    mutationFn: () => workflowsApi.activate(wfId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workflow', wfId] }),
  })

  const triggerMutation = useMutation({
    mutationFn: () => workflowsApi.trigger(wfId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['executions-workflow', wfId] }),
  })

  const deleteMutation = useMutation({
    mutationFn: () => workflowsApi.delete(wfId),
    onSuccess: () => navigate('/workflows'),
  })

  const retryMutation = useMutation({
    mutationFn: executionsApi.retry,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['executions-workflow', wfId] }),
  })

  if (wfLoading) {
    return (
      <div className="p-8">
        <div className="h-8 w-48 bg-slate-200 rounded animate-pulse mb-4" />
        <div className="h-40 bg-white rounded-xl border border-slate-200 animate-pulse" />
      </div>
    )
  }

  if (!workflow) return <div className="p-8 text-slate-500">Workflow not found.</div>

  return (
    <div className="p-8 max-w-5xl">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-slate-500 mb-6">
        <Link to="/workflows" className="hover:text-indigo-600">Workflows</Link>
        <span>/</span>
        <span className="text-slate-900 font-medium">{workflow.name}</span>
      </div>

      {/* Header Card */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 mb-6">
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <h1 className="text-xl font-bold text-slate-900">{workflow.name}</h1>
              <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[workflow.status]}`}>
                {workflow.status}
              </span>
            </div>
            {workflow.description && <p className="text-slate-500 text-sm">{workflow.description}</p>}
            <div className="flex items-center gap-4 mt-3 text-sm text-slate-500">
              <span>Trigger: <strong className="text-slate-700">{triggerLabels[workflow.triggerType]}</strong></span>
              {workflow.triggerConfig && (
                <span>Config: <code className="bg-slate-100 px-1.5 py-0.5 rounded text-xs font-mono">{workflow.triggerConfig}</code></span>
              )}
              <span>Created: {new Date(workflow.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {workflow.status !== 'ACTIVE' && (
              <button
                onClick={() => activateMutation.mutate()}
                disabled={activateMutation.isPending}
                className="bg-green-600 hover:bg-green-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
              >
                Activate
              </button>
            )}
            {workflow.status === 'ACTIVE' && workflow.triggerType === 'MANUAL' && (
              <button
                onClick={() => triggerMutation.mutate()}
                disabled={triggerMutation.isPending}
                className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
              >
                {triggerMutation.isPending ? 'Triggering…' : 'Trigger Now'}
              </button>
            )}
            <button
              onClick={() => { if (confirm('Delete this workflow?')) deleteMutation.mutate() }}
              className="border border-red-200 text-red-600 hover:bg-red-50 text-sm font-medium px-4 py-2 rounded-lg transition-colors"
            >
              Delete
            </button>
          </div>
        </div>
      </div>

      {/* Webhook URL */}
      {workflow.triggerType === 'WEBHOOK' && workflow.status === 'ACTIVE' && (
        <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-4 mb-6">
          <p className="text-sm font-medium text-indigo-700 mb-1">Webhook URL</p>
          <code className="text-xs font-mono text-indigo-900 bg-white border border-indigo-200 px-3 py-2 rounded-lg block break-all">
            {window.location.origin}/api/v1/webhooks/{workflow.id}
          </code>
          <p className="text-xs text-indigo-500 mt-1.5">POST to this URL to trigger the workflow. No authentication required.</p>
        </div>
      )}

      {/* Steps */}
      {(workflow.steps?.length ?? 0) > 0 && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 mb-6">
          <h2 className="font-semibold text-slate-900 mb-4">Steps ({workflow.steps.length})</h2>
          <div className="space-y-3">
            {workflow.steps.map((step) => (
              <div key={step.id} className="flex items-center gap-4 p-3 bg-slate-50 rounded-lg">
                <div className="w-7 h-7 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-bold shrink-0">
                  {step.stepOrder}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-800">{step.actionType.replace('_', ' ')}</p>
                  {step.actionConfig && (
                    <p className="text-xs text-slate-400 font-mono truncate mt-0.5">{step.actionConfig}</p>
                  )}
                </div>
                <span className="text-xs text-slate-400">max {step.maxRetries} retries</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Execution History */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="px-6 py-4 border-b border-slate-100">
          <h2 className="font-semibold text-slate-900">Execution History</h2>
        </div>

        {exLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-8 bg-slate-100 rounded animate-pulse" />
            ))}
          </div>
        ) : !executions?.length ? (
          <div className="py-10 text-center text-slate-400 text-sm">No executions yet</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">ID</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Status</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Trigger</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Started</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Completed</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {executions.map((ex) => (
                  <tr key={ex.id} className="hover:bg-slate-50">
                    <td className="px-6 py-3 font-mono text-slate-600">#{ex.id}</td>
                    <td className="px-6 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${execStatusColors[ex.status]}`}>
                        {ex.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-slate-500">{ex.triggerType}</td>
                    <td className="px-6 py-3 text-slate-500">
                      {ex.startedAt ? new Date(ex.startedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-3 text-slate-500">
                      {ex.completedAt ? new Date(ex.completedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-3">
                      {ex.status === 'FAILED' && (
                        <button
                          onClick={() => retryMutation.mutate(ex.id)}
                          disabled={retryMutation.isPending}
                          className="text-xs text-indigo-600 hover:text-indigo-800 font-medium disabled:opacity-50"
                        >
                          Retry
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
