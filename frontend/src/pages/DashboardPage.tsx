import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { executionsApi } from '../api/executions'
import { useAuth } from '../context/AuthContext'
import type { ExecutionStatus } from '../types'

const statusColors: Record<ExecutionStatus, string> = {
  QUEUED: 'bg-amber-100 text-amber-700',
  PENDING: 'bg-amber-100 text-amber-700',
  RUNNING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

function StatCard({ label, value, sub, accent }: { label: string; value: string | number; sub?: string; accent?: string }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
      <p className="text-sm text-slate-500 font-medium">{label}</p>
      <p className={`text-3xl font-bold mt-1 ${accent ?? 'text-slate-900'}`}>{value}</p>
      {sub && <p className="text-xs text-slate-400 mt-1">{sub}</p>}
    </div>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()

  const { data: analytics, isLoading: analyticsLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: executionsApi.getAnalytics,
    refetchInterval: 30_000,
  })

  const { data: executions, isLoading: executionsLoading } = useQuery({
    queryKey: ['my-executions'],
    queryFn: executionsApi.getMyExecutions,
  })

  const recent = executions?.slice(0, 10) ?? []

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900">
          Good to see you, {user?.username} 👋
        </h1>
        <p className="text-slate-500 mt-1">Here's what's happening with your workflows</p>
      </div>

      {/* Stats */}
      {analyticsLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-white rounded-xl border border-slate-200 p-5 h-24 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard label="Total Executions" value={analytics?.totalExecutions ?? 0} />
          <StatCard
            label="Completed"
            value={analytics?.completedExecutions ?? 0}
            accent="text-green-600"
          />
          <StatCard
            label="Failed"
            value={analytics?.failedExecutions ?? 0}
            accent={analytics?.failedExecutions ? 'text-red-600' : 'text-slate-900'}
          />
          <StatCard
            label="Success Rate"
            value={`${(analytics?.successRate ?? 0).toFixed(1)}%`}
            accent="text-indigo-600"
          />
        </div>
      )}

      {/* DLQ Alert */}
      {(analytics?.dlqCount ?? 0) > 0 && (
        <div className="mb-6 flex items-center gap-3 px-4 py-3 bg-red-50 border border-red-200 rounded-xl">
          <svg className="w-5 h-5 text-red-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p className="text-sm text-red-700 font-medium flex-1">
            {analytics!.dlqCount} message{analytics!.dlqCount > 1 ? 's' : ''} in the Dead Letter Queue
          </p>
          <Link to="/dlq" className="text-sm text-red-600 hover:text-red-800 font-semibold underline">
            View DLQ
          </Link>
        </div>
      )}

      {/* Recent Executions */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="font-semibold text-slate-900">Recent Executions</h2>
          <Link to="/executions" className="text-sm text-indigo-600 hover:text-indigo-800 font-medium">
            View all
          </Link>
        </div>

        {executionsLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-8 bg-slate-100 rounded animate-pulse" />
            ))}
          </div>
        ) : recent.length === 0 ? (
          <div className="py-12 text-center text-slate-400">
            <p className="text-sm">No executions yet.</p>
            <Link to="/workflows" className="text-indigo-600 hover:underline text-sm mt-1 inline-block">
              Create and activate a workflow →
            </Link>
          </div>
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
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recent.map((ex) => (
                  <tr key={ex.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-3 font-mono text-slate-600">#{ex.id}</td>
                    <td className="px-6 py-3 text-slate-600">
                      <Link to={`/workflows/${ex.workflowId}`} className="hover:text-indigo-600">
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
