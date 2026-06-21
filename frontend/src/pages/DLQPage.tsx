import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { executionsApi } from '../api/executions'

export default function DLQPage() {
  const qc = useQueryClient()

  const { data: entries, isLoading } = useQuery({
    queryKey: ['dlq'],
    queryFn: executionsApi.getDLQ,
    refetchInterval: 30_000,
  })

  const retryMutation = useMutation({
    mutationFn: (executionId: number) => executionsApi.retry(executionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['dlq'] })
      qc.invalidateQueries({ queryKey: ['analytics'] })
    },
  })

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Dead Letter Queue</h1>
        <p className="text-slate-500 mt-1">
          Executions that exhausted all retries. Replay them once the underlying issue is resolved.
        </p>
      </div>

      {(entries?.length ?? 0) === 0 && !isLoading && (
        <div className="bg-green-50 border border-green-200 rounded-xl p-5 flex items-center gap-3 mb-6">
          <svg className="w-5 h-5 text-green-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-green-700 text-sm font-medium">Dead Letter Queue is empty — all systems healthy.</p>
        </div>
      )}

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        {isLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-10 bg-slate-100 rounded animate-pulse" />
            ))}
          </div>
        ) : !entries?.length ? (
          <div className="py-16 text-center text-slate-400 text-sm">No dead letter entries</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">ID</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Execution</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Workflow</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Step</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Action</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Failure Reason</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Retries</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Status</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium">Created</th>
                  <th className="text-left px-6 py-3 text-slate-500 font-medium"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-slate-50">
                    <td className="px-6 py-3 font-mono text-slate-600">#{entry.id}</td>
                    <td className="px-6 py-3">
                      <Link
                        to={`/executions`}
                        className="text-indigo-600 hover:text-indigo-800 font-mono"
                      >
                        #{entry.executionId}
                      </Link>
                    </td>
                    <td className="px-6 py-3">
                      <Link
                        to={`/workflows/${entry.workflowId}`}
                        className="text-indigo-600 hover:text-indigo-800"
                      >
                        WF-{entry.workflowId}
                      </Link>
                    </td>
                    <td className="px-6 py-3 text-slate-600">{entry.stepOrder}</td>
                    <td className="px-6 py-3">
                      <span className="bg-slate-100 text-slate-700 px-2 py-0.5 rounded text-xs font-mono">
                        {entry.actionType}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-red-600 text-xs max-w-xs">
                      <span className="line-clamp-2" title={entry.failureReason}>
                        {entry.failureReason}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-center font-medium text-slate-700">{entry.retryCount}</td>
                    <td className="px-6 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                        entry.replayed
                          ? 'bg-green-100 text-green-700'
                          : 'bg-red-100 text-red-700'
                      }`}>
                        {entry.replayed ? 'Replayed' : 'Pending'}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-slate-500 text-xs">
                      {new Date(entry.createdAt).toLocaleString()}
                    </td>
                    <td className="px-6 py-3">
                      {!entry.replayed && (
                        <button
                          onClick={() => retryMutation.mutate(entry.executionId)}
                          disabled={retryMutation.isPending}
                          className="text-xs bg-indigo-600 hover:bg-indigo-700 text-white font-medium px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50"
                        >
                          Replay
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

      <p className="text-xs text-slate-400 mt-4">
        Replay re-queues the execution from the failed step. Make sure the underlying issue (e.g. email server down) is resolved first.
      </p>
    </div>
  )
}
