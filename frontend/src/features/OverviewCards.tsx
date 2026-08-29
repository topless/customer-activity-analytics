import type { CustomerOverview, TypeBreakdown } from '../api/types'
import { TypeChip } from '../components/badges'
import { formatAmount, formatDate, formatInt } from '../lib/format'

function TypeCard({ breakdown }: { breakdown: TypeBreakdown }) {
  return (
    <div className="card summary-card">
      <div className="summary-label">
        <span>{breakdown.activityType.toLowerCase()} activity</span>
        <TypeChip type={breakdown.activityType} />
      </div>
      <div className="summary-value num">{formatInt(breakdown.count)}</div>
      <div className="summary-sub">
        {breakdown.failedCount > 0 ? (
          <span className="summary-failed">{formatInt(breakdown.failedCount)} failed</span>
        ) : (
          'no failures'
        )}
      </div>
      {breakdown.totalsByCurrency.length > 0 ? (
        <div className="summary-volumes">
          {breakdown.totalsByCurrency.map((total) => (
            <div className="summary-volume" key={total.currency}>
              <span>{total.currency}</span>
              <span className="amount">{formatAmount(total.totalAmount)}</span>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  )
}

/** Summary cards row: transaction totals + one card per activity type present. */
export function OverviewCards({ overview }: { overview: CustomerOverview }) {
  const failedTotal = overview.byStatus.find((s) => s.status === 'FAILED')?.count ?? 0

  return (
    <div className="summary-grid">
      <div className="card summary-card">
        <div className="summary-label">
          <span>Transactions</span>
        </div>
        <div className="summary-value num">{formatInt(overview.transactionCount)}</div>
        <div className="summary-sub">
          {overview.windowFrom && overview.windowTo
            ? `${formatDate(overview.windowFrom)} — ${formatDate(overview.windowTo)}`
            : 'no activity window'}
        </div>
        {overview.byStatus.length > 0 ? (
          <div className="summary-volumes">
            {overview.byStatus.map((status) => (
              <div className="summary-volume" key={status.status}>
                <span className={status.status === 'FAILED' && failedTotal > 0 ? 'summary-failed' : undefined}>
                  {status.status.toLowerCase()}
                </span>
                <span className="amount">{formatInt(status.count)}</span>
              </div>
            ))}
          </div>
        ) : null}
      </div>
      {overview.byType.map((breakdown) => (
        <TypeCard key={breakdown.activityType} breakdown={breakdown} />
      ))}
    </div>
  )
}
