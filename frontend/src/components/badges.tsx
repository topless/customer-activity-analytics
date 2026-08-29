import type { ActivityType, RiskLevel, RuleAppliesTo, TransactionStatus } from '../api/types'
import { formatScore } from '../lib/format'
import { levelBand, scoreBand } from '../lib/risk'

/** Colored pill for a numeric risk score, per the shared banding convention. */
export function RiskScoreBadge({ score, title }: { score: number; title?: string }) {
  const band = scoreBand(score)
  return (
    <span className={`risk-badge band-${band}`} title={title ?? `Risk score ${formatScore(score)}`}>
      <span className="dot" aria-hidden="true" />
      {formatScore(score)}
    </span>
  )
}

/** Colored pill for a LOW / MEDIUM / HIGH / CRITICAL level (severities are a subset). */
export function LevelBadge({ level }: { level: RiskLevel }) {
  return (
    <span className={`risk-badge band-${levelBand(level)}`}>
      <span className="dot" aria-hidden="true" />
      {level}
    </span>
  )
}

const STATUS_CLASS: Record<TransactionStatus, string> = {
  COMPLETED: 'band-low',
  PENDING: 'band-medium',
  FAILED: 'band-critical',
  REVERSED: 'chip-reversed',
}

export function StatusChip({ status }: { status: TransactionStatus }) {
  return <span className={`chip ${STATUS_CLASS[status] ?? 'chip-neutral'}`}>{status}</span>
}

const SERIES_VAR: Record<ActivityType, string> = {
  CARD: 'var(--series-card)',
  PAYMENT: 'var(--series-payment)',
  CRYPTO: 'var(--series-crypto)',
}

/** Neutral chip with a series-colored dot — ties table rows to the chart legend. */
export function TypeChip({ type }: { type: ActivityType }) {
  return (
    <span className="chip chip-outline">
      <span className="dot" style={{ background: SERIES_VAR[type] }} aria-hidden="true" />
      {type}
    </span>
  )
}

/** Applies-to chip for risk rules: an activity type, or ALL. */
export function AppliesToChip({ appliesTo }: { appliesTo: RuleAppliesTo }) {
  if (appliesTo === 'ALL') return <span className="chip chip-neutral">ALL</span>
  return <TypeChip type={appliesTo} />
}

export function KycBadge({ level }: { level: string }) {
  const cls =
    level === 'ENHANCED' ? 'chip-accent' : level === 'BASIC' ? 'chip-outline' : 'chip-neutral'
  return (
    <span className={`chip ${cls}`} title={`KYC level: ${level}`}>
      KYC · {level}
    </span>
  )
}
