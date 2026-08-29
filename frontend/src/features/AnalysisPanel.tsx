import { useCallback, useEffect, useRef, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { AnalysisDetail, AnalysisSummary } from '../api/types'
import { LevelBadge } from '../components/badges'
import { EmptyState, ErrorAlert, LoadingBlock, Spinner } from '../components/ui'
import { formatDateTime } from '../lib/format'
import { levelBand } from '../lib/risk'
import './analysis.css'

const LEVEL_CAPTION: Record<string, string> = {
  LOW: 'No significant concerns identified',
  MEDIUM: 'Some patterns warrant monitoring',
  HIGH: 'Elevated risk — review recommended',
  CRITICAL: 'Serious concerns — immediate action recommended',
}

function RunningState({ startedAt }: { startedAt: number }) {
  const [elapsed, setElapsed] = useState(0)

  useEffect(() => {
    const timer = window.setInterval(
      () => setElapsed(Math.floor((Date.now() - startedAt) / 1000)),
      1000,
    )
    return () => window.clearInterval(timer)
  }, [startedAt])
  return (
    <div className="analysis-running" role="status">
      <Spinner large />
      <div className="analysis-running-title">Running AI analysis…</div>
      <div>
        The model is reviewing this customer&rsquo;s activity against policy. This can take up to
        two minutes.
      </div>
      <div className="analysis-running-elapsed">{elapsed}s elapsed</div>
    </div>
  )
}

function AnalysisDetailView({ analysis }: { analysis: AnalysisDetail }) {
  if (analysis.status === 'FAILED') {
    return (
      <>
        <ErrorAlert
          message={`Analysis failed: ${analysis.errorMessage ?? 'no error message recorded.'}`}
        />
        <p className="faint" style={{ marginTop: 10, fontSize: 12.5 }}>
          Requested by {analysis.requestedBy.displayName} on {formatDateTime(analysis.requestedAt)}{' '}
          · model {analysis.model}
        </p>
      </>
    )
  }

  const level = analysis.riskLevel
  const findings = analysis.findings ?? []
  const recommendations = analysis.recommendations ?? []
  const citedPolicies = analysis.citedPolicies ?? []

  return (
    <>
      {level ? (
        <div className={`analysis-banner band-${levelBand(level)}`}>
          <div className="analysis-banner-level">
            <span className="dot" aria-hidden="true" />
            <div>
              <div className="analysis-banner-title">{level} RISK</div>
              <div className="analysis-banner-caption">{LEVEL_CAPTION[level]}</div>
            </div>
          </div>
          <div className="analysis-banner-meta">
            {analysis.completedAt ? <div>{formatDateTime(analysis.completedAt)}</div> : null}
            <div>
              {analysis.transactionCount} transactions · <span className="mono">{analysis.model}</span>
            </div>
            <div>Requested by {analysis.requestedBy.displayName}</div>
          </div>
        </div>
      ) : null}

      {analysis.summary ? <p className="analysis-summary">{analysis.summary}</p> : null}

      {findings.length > 0 ? (
        <section className="analysis-section">
          <h3 className="analysis-section-title">Findings ({findings.length})</h3>
          {findings.map((finding, index) => (
            <div className="finding" key={`${finding.title}-${index}`}>
              <div className="finding-badge">
                <LevelBadge level={finding.severity} />
              </div>
              <div>
                <div className="finding-title">{finding.title}</div>
                <div className="finding-detail">{finding.detail}</div>
                {finding.relatedTransactionIds.length > 0 ? (
                  <div className="finding-related">
                    {finding.relatedTransactionIds.length} related transaction
                    {finding.relatedTransactionIds.length === 1 ? '' : 's'}
                  </div>
                ) : null}
              </div>
            </div>
          ))}
        </section>
      ) : null}

      {recommendations.length > 0 ? (
        <section className="analysis-section">
          <h3 className="analysis-section-title">Recommendations</h3>
          <ul className="recommendation-list">
            {recommendations.map((recommendation) => (
              <li className="recommendation" key={recommendation}>
                <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <rect x="1.7" y="1.7" width="12.6" height="12.6" rx="3.4" stroke="currentColor" strokeWidth="1.5" />
                  <path d="m5 8.2 2.1 2.1L11 5.9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                <span>{recommendation}</span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {citedPolicies.length > 0 ? (
        <section className="analysis-section" style={{ marginBottom: 0 }}>
          <h3 className="analysis-section-title">Cited policies</h3>
          {citedPolicies.map((policy) => (
            <blockquote className="policy-quote" key={policy.chunkId}>
              <div className="policy-source">
                {policy.documentTitle} <span className="section">— {policy.sectionTitle}</span>
              </div>
              <div className="policy-excerpt">“{policy.excerpt}”</div>
            </blockquote>
          ))}
        </section>
      ) : null}
    </>
  )
}

export function AnalysisPanel({ customerId }: { customerId: string }) {
  const [history, setHistory] = useState<AnalysisSummary[] | null>(null)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [selected, setSelected] = useState<AnalysisDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [running, setRunning] = useState(false)
  const [runStartedAt, setRunStartedAt] = useState(0)
  const [runError, setRunError] = useState<string | null>(null)
  const autoLoadedRef = useRef(false)

  const loadDetail = useCallback((analysisId: string) => {
    setDetailLoading(true)
    setDetailError(null)
    api
      .getAnalysis(analysisId)
      .then((detail) => setSelected(detail))
      .catch((cause: unknown) => setDetailError(errorMessage(cause)))
      .finally(() => setDetailLoading(false))
  }, [])

  // Load the analysis history; auto-open the latest one on first load.
  useEffect(() => {
    const controller = new AbortController()
    api
      .listAnalyses(customerId, controller.signal)
      .then((list) => {
        setHistory(list)
        const latest = list[0]
        if (!autoLoadedRef.current && latest) {
          autoLoadedRef.current = true
          loadDetail(latest.id)
        }
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setHistoryError(errorMessage(cause))
      })
    return () => controller.abort()
  }, [customerId, loadDetail])

  async function runAnalysis() {
    if (running) return
    setRunning(true)
    setRunStartedAt(Date.now())
    setRunError(null)
    setDetailError(null)
    try {
      // Synchronous on the server; can take up to ~2 minutes with a real LLM.
      const result = await api.runAnalysis(customerId)
      setSelected(result)
      // Refresh the history so the new run appears at the top.
      const list = await api.listAnalyses(customerId)
      setHistory(list)
    } catch (cause) {
      setRunError(errorMessage(cause))
    } finally {
      setRunning(false)
    }
  }

  return (
    <section className="card" aria-label="AI risk analysis">
      <div className="card-head">
        <h2 className="card-title">AI risk analysis</h2>
        <button
          type="button"
          className="btn btn-primary"
          onClick={runAnalysis}
          disabled={running}
        >
          {running ? <Spinner /> : (
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M8 1.6 9.7 5.9l4.5.4-3.4 3 1 4.4L8 11.4l-3.8 2.3 1-4.4-3.4-3 4.5-.4z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
            </svg>
          )}
          {running ? 'Analyzing…' : 'Run AI analysis'}
        </button>
      </div>

      <div className="analysis-grid">
        <div className="analysis-main">
          {runError ? (
            <div style={{ marginBottom: 14 }}>
              <ErrorAlert message={runError} />
            </div>
          ) : null}

          {running ? (
            <RunningState startedAt={runStartedAt} />
          ) : detailLoading ? (
            <LoadingBlock label="Loading analysis…" />
          ) : detailError ? (
            <ErrorAlert message={detailError} />
          ) : selected ? (
            <AnalysisDetailView analysis={selected} />
          ) : history !== null && history.length === 0 ? (
            <EmptyState
              title="No analyses yet"
              hint="Run the first AI analysis to get a policy-grounded risk assessment of this customer’s activity."
            />
          ) : historyError ? (
            <EmptyState
              title="Analyses unavailable"
              hint="The analysis history could not be loaded. See the error in the History panel."
            />
          ) : (
            <LoadingBlock label="Loading analyses…" />
          )}
        </div>

        <aside className="analysis-history" aria-label="Analysis history">
          <div className="analysis-history-title">History</div>
          {historyError ? (
            <div style={{ padding: '0 16px 14px' }}>
              <ErrorAlert message={historyError} />
            </div>
          ) : history === null ? (
            <LoadingBlock label="Loading…" />
          ) : history.length === 0 ? (
            <p className="faint" style={{ padding: '2px 16px 14px', fontSize: 12.5 }}>
              Completed and failed runs will appear here.
            </p>
          ) : (
            <div className="analysis-history-list">
              {history.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={selected?.id === item.id ? 'history-row active' : 'history-row'}
                  onClick={() => loadDetail(item.id)}
                >
                  <div className="history-row-top">
                    <span className="history-row-when">{formatDateTime(item.requestedAt)}</span>
                    {item.status === 'FAILED' ? (
                      <span className="chip band-critical">FAILED</span>
                    ) : item.riskLevel ? (
                      <LevelBadge level={item.riskLevel} />
                    ) : null}
                  </div>
                  <div className="history-row-meta">
                    <span>{item.requestedBy.displayName}</span>
                    <span aria-hidden="true">·</span>
                    <span className="history-row-model">{item.model}</span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}
