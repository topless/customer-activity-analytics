import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, api, errorMessage } from '../api/client'
import type { CustomerOverview, CustomerProfile } from '../api/types'
import { KycBadge } from '../components/badges'
import { EmptyState, ErrorAlert, LoadingBlock } from '../components/ui'
import { AnalysisPanel } from '../features/AnalysisPanel'
import { MonthlyChart, ChartLegend } from '../features/MonthlyChart'
import { OverviewCards } from '../features/OverviewCards'
import { TransactionsTable } from '../features/TransactionsTable'
import { TriggeredRulesPanel } from '../features/TriggeredRulesPanel'
import { formatDate, formatScore } from '../lib/format'
import { scoreBand } from '../lib/risk'
import './customer.css'

function MetaIcon({ path }: { path: string }) {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d={path} stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function CustomerPage() {
  const { id } = useParams<{ id: string }>()
  // Results keyed by customer id so switching customers never shows stale data.
  const [profileResult, setProfileResult] = useState<{ id: string; profile: CustomerProfile } | null>(null)
  const [profileFailure, setProfileFailure] = useState<{ id: string; notFound: boolean; message: string } | null>(null)
  const [overviewResult, setOverviewResult] = useState<{ id: string; overview: CustomerOverview } | null>(null)
  const [overviewFailure, setOverviewFailure] = useState<{ id: string; message: string } | null>(null)

  useEffect(() => {
    if (!id) return
    const controller = new AbortController()

    api
      .getCustomer(id, controller.signal)
      .then((profile) => setProfileResult({ id, profile }))
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        const notFound = cause instanceof ApiError && cause.status === 404
        setProfileFailure({ id, notFound, message: errorMessage(cause) })
      })

    api
      .getOverview(id, controller.signal)
      .then((overview) => setOverviewResult({ id, overview }))
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setOverviewFailure({ id, message: errorMessage(cause) })
      })

    return () => controller.abort()
  }, [id])

  const profile =
    id !== undefined && profileResult?.id === id ? profileResult.profile : null
  const failure = id !== undefined && profileFailure?.id === id ? profileFailure : null
  const notFound = failure?.notFound ?? false
  const profileError = failure !== null && !failure.notFound ? failure.message : null
  const overview =
    id !== undefined && overviewResult?.id === id ? overviewResult.overview : null
  const overviewError =
    id !== undefined && overviewFailure?.id === id ? overviewFailure.message : null

  if (!id || notFound) {
    return (
      <div className="page-state">
        <EmptyState
          title="Customer not found"
          hint="There is no customer with this id. It may have been mistyped or removed."
        >
          <Link to="/" className="btn btn-ghost" style={{ marginTop: 10 }}>
            Back to customer search
          </Link>
        </EmptyState>
      </div>
    )
  }

  if (profileError) {
    return (
      <>
        <Link to="/" className="back-link">
          ← Customers
        </Link>
        <ErrorAlert message={profileError} />
      </>
    )
  }

  if (!profile) {
    return <LoadingBlock label="Loading customer…" />
  }

  const riskScore = overview?.riskScore

  return (
    <>
      <Link to="/" className="back-link">
        ← Customers
      </Link>

      <header className="card customer-head">
        <div>
          <h1 className="customer-title">
            {profile.fullName}
            <KycBadge level={profile.kycLevel} />
          </h1>
          <div className="customer-meta">
            <span className="meta-item mono" style={{ fontWeight: 600 }}>
              {profile.customerNumber}
            </span>
            <span className="meta-item">
              <MetaIcon path="M8 8.7a2.6 2.6 0 1 0 0-5.2 2.6 2.6 0 0 0 0 5.2Zm-5 5.6c.7-2.4 2.7-3.7 5-3.7s4.3 1.3 5 3.7" />
              {profile.country}
            </span>
            <a className="meta-item" href={`mailto:${profile.email}`}>
              <MetaIcon path="M2.5 4.5h11v7h-11zM2.8 5l5.2 4 5.2-4" />
              {profile.email}
            </a>
            <span className="meta-item" title="Date of birth">
              <MetaIcon path="M8 2.2v2M4.7 3v1.6M11.3 3v1.6M2.8 6.7h10.4M3.5 5h9a1 1 0 0 1 1 1v6.5a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" />
              Born {formatDate(profile.dateOfBirth)}
            </span>
            <span className="meta-item" title="Onboarding date">
              <MetaIcon path="M8 13.8A5.8 5.8 0 1 0 8 2.2a5.8 5.8 0 0 0 0 11.6ZM8 5v3.2l2.2 1.3" />
              Onboarded {formatDate(profile.onboardedAt)}
            </span>
          </div>
        </div>
        {riskScore !== undefined ? (
          <div className={`risk-hero band-${scoreBand(riskScore)}`}>
            <span className="risk-hero-label">Risk score</span>
            <span className="risk-hero-value">{formatScore(riskScore)}</span>
          </div>
        ) : null}
      </header>

      {overviewError ? (
        <div style={{ marginBottom: 16 }}>
          <ErrorAlert message={`Activity overview unavailable: ${overviewError}`} />
        </div>
      ) : overview ? (
        <>
          <OverviewCards overview={overview} />
          <div className="dash-grid">
            <section className="card" aria-label="Monthly activity">
              <div className="card-head">
                <h2 className="card-title">Monthly activity</h2>
                <ChartLegend />
              </div>
              <MonthlyChart months={overview.monthlyCounts} />
            </section>
            <TriggeredRulesPanel rules={overview.triggeredRules} />
          </div>
        </>
      ) : (
        <div className="card" style={{ marginBottom: 16 }}>
          <LoadingBlock label="Loading activity overview…" />
        </div>
      )}

      <div className="stack-16">
        {/* Keyed by customer id: navigating to another customer resets filters,
            expanded rows and the selected analysis. */}
        <TransactionsTable key={id} customerId={id} />
        <AnalysisPanel key={`analysis-${id}`} customerId={id} />
      </div>
    </>
  )
}
