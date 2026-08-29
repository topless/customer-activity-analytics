import type { TriggeredRule } from '../api/types'
import { AppliesToChip } from '../components/badges'
import { EmptyState } from '../components/ui'
import { formatDate, formatScore } from '../lib/format'

/** Risk rules this customer has triggered, in the order delivered by the API
 *  (sorted by total contribution, descending). */
export function TriggeredRulesPanel({ rules }: { rules: TriggeredRule[] }) {
  return (
    <section className="card" aria-label="Triggered risk rules">
      <div className="card-head">
        <h2 className="card-title">Triggered risk rules</h2>
        <span className="chip chip-neutral num">{rules.length}</span>
      </div>
      {rules.length === 0 ? (
        <EmptyState
          title="No rules triggered"
          hint="None of this customer’s transactions have tripped a risk rule."
        />
      ) : (
        <div className="rules-list">
          {rules.map((rule) => (
            <div className="rule-row" key={rule.ruleId}>
              <div className="rule-top">
                <span className="rule-name">{rule.ruleName}</span>
                <span className="rule-contribution" title="Total risk score contribution">
                  +{formatScore(rule.totalContribution)}
                </span>
              </div>
              <div className="rule-meta">
                <AppliesToChip appliesTo={rule.appliesTo} />
                <span className="num">
                  {rule.timesTriggered}× triggered
                </span>
                <span>last {formatDate(rule.lastTriggeredAt)}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
