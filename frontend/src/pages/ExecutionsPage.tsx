import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { executionsApi } from '../api/executions'
import type { ExecutionStatus } from '../types'

const ALL_STATUSES: (ExecutionStatus | 'ALL')[] = ['ALL', 'RUNNING', 'COMPLETED', 'FAILED', 'QUEUED']

const statusColors: Record<ExecutionStatus, string> = {
  QUEUED: 'bg-amber-100 text-amber-700',
  PENDING: 'bg-amber-100 text-amber-700',
  RUNNING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

export default function ExecutionsPage() {
  const qc = useQueryClient()
  const [activeFilter, setActiveFilter] = useState<ExecutionStatus | 'ALL'>('ALL')
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const { data: allExecutions, isLoading } = useQuery({
    queryKey: ['my-executions'],
    queryFn: executionsApi.getMyExecutions,
    refetchInterval: 15_000,
  })

  const { data: filtered } = useQuery({
    queryKey: ['executions-filtered', activeFilter],
    queryFn: () => executionsApi.filterByStatus(activeFilter as ExecutionStatus),
    enabled: activeFilter !== 'ALL',
  })

  const retryMutation = useMutation({
    mutationFn: executionsApi.retry,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-executions'] })
      qc.invalidateQueries({ queryKey: ['executions-filtered'] })
    },
  })

  const executions = activeFilter === 'ALL' ? allExecutions : filtered

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Executions</h1>
        <p className="text-slate-500 mt-1">Monitor all workflow execution history</p>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-1 p-1 bg-slate-100 rounded-lg mb-6 w-fit">
        {ALL_STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setActiveFilter(s)}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
              activeFilter === s
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        {isLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-8 bg-slate-100 rounded animate-pulse" />
            ))}
          </div>
        ) : !executions?.length ? (
          <div className="py-16 text-center text-slate-400 text-sm">No executions found</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">ID</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Workflow</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Status</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Trigger</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Started</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Error</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {executions.map((ex) => (
                  <>
                    <tr
                      key={ex.id}
                      className="hover:bg-slate-50 cursor-pointer"
                      onClick={() => setExpandedId(expandedId === ex.id ? null : ex.id)}
                    >
                      <td className="px-6 py-3 font-mono text-slate-600">#{ex.id}</td>
                      <td className="px-6 py-3">
                        <Link
                          to={`/workflows/${ex.workflowId}`}
                          onClick={(e) => e.stopPropagation()}
                          className="text-indigo-600 hover:text-indigo-800 font-medium"
                        >
                          WF-{ex.workflowId}
                        </Link>
                      </td>
                      <td className="px-6 py-3">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[ex.status]}`}>
                          {ex.status}
                        </span>
                      </td>
                      <td className="px-6 py-3 text-slate-500">{ex.triggerType}</td>
                      <td className="px-6 py-3 text-slate-500">
                        {ex.startedAt ? new Date(ex.startedAt).toLocaleString() : '—'}
                      </td>
                      <td className="px-6 py-3 text-red-500 text-xs max-w-xs truncate">
                        {ex.errorMessage ?? '—'}
                      </td>
                      <td className="px-6 py-3">
                        {ex.status === 'FAILED' && (
                          <button
                            onClick={(e) => { e.stopPropagation(); retryMutation.mutate(ex.id) }}
                            disabled={retryMutation.isPending}
                            className="text-xs text-indigo-600 hover:text-indigo-800 font-medium disabled:opacity-50"
                          >
                            Retry
                          </button>
                        )}
                      </td>
                    </tr>

                    {/* Expanded Step Details */}
                    {expandedId === ex.id && ex.steps?.length > 0 && (
                      <tr key={`${ex.id}-steps`}>
                        <td colSpan={7} className="px-6 pb-4 pt-0 bg-slate-50">
                          <div className="space-y-2 mt-1">
                            {ex.steps.map((step) => (
                              <div key={step.id} className="flex items-center gap-3 p-2.5 bg-white rounded-lg border border-slate-200 text-xs">
                                <div className="w-5 h-5 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center font-bold shrink-0">
                                  {step.stepOrder}
                                </div>
                                <span className="font-medium text-slate-700 w-32 shrink-0">{step.actionType}</span>
                                <span className={`px-1.5 py-0.5 rounded-full font-medium ${
                                  step.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                                  step.status === 'FAILED' ? 'bg-red-100 text-red-700' :
                                  'bg-amber-100 text-amber-700'
                                }`}>
                                  {step.status}
                                </span>
                                {step.errorMessage && (
                                  <span className="text-red-500 truncate">{step.errorMessage}</span>
                                )}
                                {step.executedAt && (
                                  <span className="text-slate-400 ml-auto shrink-0">
                                    {new Date(step.executedAt).toLocaleTimeString()}
                                  </span>
                                )}
                              </div>
                            ))}
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
