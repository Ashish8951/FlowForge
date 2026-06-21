import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workflowsApi } from '../api/workflows'
import type { CreateWorkflowRequest, TriggerType, WorkflowStatus } from '../types'

const statusColors: Record<WorkflowStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  INACTIVE: 'bg-slate-100 text-slate-600',
  DRAFT: 'bg-amber-100 text-amber-700',
}

const triggerLabels: Record<TriggerType, string> = {
  WEBHOOK: 'Webhook',
  SCHEDULE: 'Schedule (CRON)',
  MANUAL: 'Manual',
}

const defaultForm: CreateWorkflowRequest = {
  name: '',
  description: '',
  triggerType: 'MANUAL',
  triggerConfig: '',
}

export default function WorkflowsPage() {
  const qc = useQueryClient()
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState<CreateWorkflowRequest>(defaultForm)
  const [formError, setFormError] = useState('')

  const { data: workflows, isLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: workflowsApi.getAll,
  })

  const createMutation = useMutation({
    mutationFn: workflowsApi.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflows'] })
      setShowModal(false)
      setForm(defaultForm)
    },
    onError: (err: any) => setFormError(err?.response?.data?.message ?? 'Failed to create workflow.'),
  })

  const activateMutation = useMutation({
    mutationFn: workflowsApi.activate,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workflows'] }),
  })

  const deactivateMutation = useMutation({
    mutationFn: workflowsApi.deactivate,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workflows'] }),
  })

  const triggerMutation = useMutation({
    mutationFn: workflowsApi.trigger,
  })

  const deleteMutation = useMutation({
    mutationFn: workflowsApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workflows'] }),
  })

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault()
    setFormError('')
    const payload: CreateWorkflowRequest = { ...form }
    // Wrap raw cron expression in JSON object expected by the backend
    if (form.triggerType === 'SCHEDULE' && form.triggerConfig) {
      payload.triggerConfig = JSON.stringify({ cron: form.triggerConfig })
    }
    createMutation.mutate(payload)
  }

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Workflows</h1>
          <p className="text-slate-500 mt-1">Manage and trigger your automation workflows</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium px-4 py-2.5 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Workflow
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-white rounded-xl border border-slate-200 p-5 h-40 animate-pulse" />
          ))}
        </div>
      ) : workflows?.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-xl border border-slate-200">
          <svg className="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <p className="text-slate-500 font-medium">No workflows yet</p>
          <p className="text-slate-400 text-sm mt-1">Create your first workflow to get started</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {workflows?.map((wf) => (
            <div key={wf.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col gap-3">
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-slate-900 truncate">{wf.name}</h3>
                  {wf.description && (
                    <p className="text-slate-500 text-sm mt-0.5 line-clamp-1">{wf.description}</p>
                  )}
                </div>
                <span className={`ml-2 shrink-0 inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[wf.status]}`}>
                  {wf.status}
                </span>
              </div>

              <div className="flex items-center gap-1.5 text-xs text-slate-500">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                {triggerLabels[wf.triggerType]}
                <span className="mx-1">·</span>
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                </svg>
                {wf.steps?.length ?? 0} step{(wf.steps?.length ?? 0) !== 1 ? 's' : ''}
              </div>

              <div className="flex items-center gap-2 pt-1 border-t border-slate-100">
                <Link
                  to={`/workflows/${wf.id}`}
                  className="flex-1 text-center text-xs font-medium text-slate-600 hover:text-indigo-600 py-1.5 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  View
                </Link>
                {wf.status !== 'ACTIVE' && (
                  <button
                    onClick={() => activateMutation.mutate(wf.id)}
                    disabled={activateMutation.isPending}
                    className="flex-1 text-center text-xs font-medium text-green-600 hover:text-green-700 py-1.5 rounded-lg hover:bg-green-50 transition-colors disabled:opacity-50"
                  >
                    Activate
                  </button>
                )}
                {wf.status === 'ACTIVE' && (
                  <button
                    onClick={() => deactivateMutation.mutate(wf.id)}
                    disabled={deactivateMutation.isPending}
                    className="flex-1 text-center text-xs font-medium text-slate-500 hover:text-slate-700 py-1.5 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
                  >
                    Pause
                  </button>
                )}
                {wf.status === 'ACTIVE' && wf.triggerType === 'MANUAL' && (
                  <button
                    onClick={() => triggerMutation.mutate(wf.id)}
                    disabled={triggerMutation.isPending}
                    className="flex-1 text-center text-xs font-medium text-indigo-600 hover:text-indigo-700 py-1.5 rounded-lg hover:bg-indigo-50 transition-colors disabled:opacity-50"
                  >
                    Trigger
                  </button>
                )}
                <button
                  onClick={() => {
                    if (confirm('Delete this workflow?')) deleteMutation.mutate(wf.id)
                  }}
                  className="text-xs font-medium text-red-500 hover:text-red-600 py-1.5 px-2 rounded-lg hover:bg-red-50 transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h2 className="text-lg font-semibold text-slate-900">New Workflow</h2>
              <button onClick={() => { setShowModal(false); setFormError('') }} className="text-slate-400 hover:text-slate-600">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <form onSubmit={handleCreate} className="p-6 space-y-4">
              {formError && (
                <div className="px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                  {formError}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Name *</label>
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  className="w-full border border-slate-300 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Payment Failure Handler"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
                <input
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  className="w-full border border-slate-300 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Optional description"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Trigger Type *</label>
                <select
                  value={form.triggerType}
                  onChange={(e) => setForm((f) => ({ ...f, triggerType: e.target.value as TriggerType, triggerConfig: '' }))}
                  className="w-full border border-slate-300 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="MANUAL">Manual (API)</option>
                  <option value="WEBHOOK">Webhook</option>
                  <option value="SCHEDULE">Schedule (CRON)</option>
                </select>
              </div>
              {form.triggerType === 'SCHEDULE' && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">CRON Expression</label>
                  <input
                    value={form.triggerConfig}
                    onChange={(e) => setForm((f) => ({ ...f, triggerConfig: e.target.value }))}
                    className="w-full border border-slate-300 rounded-lg px-3.5 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="0 9 * * MON-FRI"
                  />
                  <p className="text-xs text-slate-400 mt-1">e.g. every weekday at 9am</p>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => { setShowModal(false); setFormError('') }}
                  className="flex-1 border border-slate-300 text-slate-700 text-sm font-medium py-2.5 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium py-2.5 rounded-lg transition-colors"
                >
                  {createMutation.isPending ? 'Creating…' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
