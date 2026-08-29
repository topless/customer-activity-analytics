import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../api/client'
import type { CustomerSummary, Page } from '../api/types'
import { KycBadge, RiskScoreBadge } from '../components/badges'
import { EmptyState, ErrorAlert, LoadingBlock, Pager } from '../components/ui'
import { formatDateTime, formatInt } from '../lib/format'
import { useDebouncedValue } from '../lib/useDebouncedValue'
import './search.css'

const PAGE_SIZE = 20

export function SearchPage() {
  const navigate = useNavigate()
  const [input, setInput] = useState('')
  const [page, setPage] = useState(0)
  const query = useDebouncedValue(input.trim(), 300)

  // Results are keyed by their request parameters: loading/error states are
  // derived from key (mis)matches, and stale results stay visible, dimmed.
  const requestKey = `${query}|${page}`
  const [result, setResult] = useState<{ key: string; page: Page<CustomerSummary> } | null>(null)
  const [failure, setFailure] = useState<{ key: string; message: string } | null>(null)

  useEffect(() => {
    const key = `${query}|${page}`
    const controller = new AbortController()
    api
      .searchCustomers({ query, page, size: PAGE_SIZE }, controller.signal)
      .then((response) => setResult({ key, page: response }))
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setFailure({ key, message: errorMessage(cause) })
      })
    return () => controller.abort()
  }, [query, page])

  const data = result?.page ?? null
  const error = failure?.key === requestKey ? failure.message : null
  const loading = result?.key !== requestKey && error === null
  const showInitialLoading = loading && data === null

  return (
    <>
      <div className="search-header">
        <div>
          <h1 className="page-title">Customers</h1>
          <p className="page-subtitle">
            {data ? `${formatInt(data.totalElements)} customers` : 'Search the customer base'}
          </p>
        </div>
        <div className="search-box">
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="7" cy="7" r="4.6" stroke="currentColor" strokeWidth="1.6" />
            <path d="m10.6 10.6 3 3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
          </svg>
          <input
            type="search"
            className="input"
            placeholder="Search by number, name or UUID…"
            aria-label="Search customers"
            value={input}
            onChange={(e) => {
              setInput(e.target.value)
              setPage(0)
            }}
            autoFocus
          />
        </div>
      </div>

      <div className="card">
        {error ? (
          <div className="card-body">
            <ErrorAlert message={error} />
          </div>
        ) : showInitialLoading ? (
          <LoadingBlock label="Loading customers…" />
        ) : data && data.content.length === 0 ? (
          <EmptyState
            title="No customers found"
            hint={
              query
                ? `Nothing matches “${query}”. Try a customer number (CUST-…), a name, or a full UUID.`
                : 'The customer base is empty.'
            }
          />
        ) : data ? (
          <>
            <div className="table-wrap">
              <table className={loading ? 'table table-loading' : 'table'}>
                <thead>
                  <tr>
                    <th>Customer</th>
                    <th>Name</th>
                    <th>Country</th>
                    <th>KYC</th>
                    <th className="th-num">Transactions</th>
                    <th>Last activity</th>
                    <th className="th-num">Risk score</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((customer) => (
                    <tr
                      key={customer.id}
                      className="row-link"
                      onClick={() => navigate(`/customers/${customer.id}`)}
                    >
                      <td className="mono nowrap">
                        <Link
                          to={`/customers/${customer.id}`}
                          onClick={(e) => e.stopPropagation()}
                          style={{ fontWeight: 600 }}
                        >
                          {customer.customerNumber}
                        </Link>
                      </td>
                      <td>
                        <div className="customer-name">{customer.fullName}</div>
                        <div className="customer-email">{customer.email}</div>
                      </td>
                      <td>
                        <span className="country-chip">{customer.country}</span>
                      </td>
                      <td>
                        <KycBadge level={customer.kycLevel} />
                      </td>
                      <td className="td-num">{formatInt(customer.transactionCount)}</td>
                      <td className="nowrap muted">
                        {customer.lastActivityAt ? formatDateTime(customer.lastActivityAt) : '—'}
                      </td>
                      <td className="td-num">
                        <RiskScoreBadge score={customer.riskScore} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pager page={data} onPage={setPage} label="customers" />
          </>
        ) : null}
      </div>
    </>
  )
}
