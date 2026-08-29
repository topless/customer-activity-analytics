import type { ReactNode } from 'react'
import type { Page } from '../api/types'
import { formatInt } from '../lib/format'

export function Spinner({ large = false }: { large?: boolean }) {
  return <span className={large ? 'spinner spinner-lg' : 'spinner'} role="status" aria-label="Loading" />
}

export function LoadingBlock({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading-block">
      <Spinner large />
      <span>{label}</span>
    </div>
  )
}

export function ErrorAlert({ message }: { message: string }) {
  return (
    <div className="alert alert-error" role="alert">
      <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <circle cx="8" cy="8" r="6.6" stroke="currentColor" strokeWidth="1.5" />
        <path d="M8 4.8v3.9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="8" cy="11.2" r="0.9" fill="currentColor" />
      </svg>
      <span>{message}</span>
    </div>
  )
}

export function EmptyState({ title, hint, children }: { title: string; hint?: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <svg className="empty-icon" width="34" height="34" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <rect x="3.2" y="5.2" width="17.6" height="13.6" rx="2.2" stroke="currentColor" strokeWidth="1.4" />
        <path d="M3.2 9.4h17.6" stroke="currentColor" strokeWidth="1.4" />
        <path d="M6.6 13.4h6" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        <path d="M6.6 16.1h3.4" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
      <div className="empty-title">{title}</div>
      {hint ? <div className="empty-hint">{hint}</div> : null}
      {children}
    </div>
  )
}

/** Footer pagination for the server-side paging envelope. */
export function Pager({
  page,
  onPage,
  label = 'transactions',
}: {
  page: Page<unknown>
  onPage: (page: number) => void
  label?: string
}) {
  const { page: current, size, totalElements, totalPages } = page
  if (totalElements === 0) return null

  const from = current * size + 1
  const to = Math.min((current + 1) * size, totalElements)

  // Compact page list: first, last, and a window around the current page.
  const pages: number[] = []
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - current) <= 1) pages.push(i)
  }

  return (
    <nav className="pager" aria-label="Pagination">
      <span className="num">
        {formatInt(from)}–{formatInt(to)} of {formatInt(totalElements)} {label}
      </span>
      {totalPages > 1 ? (
        <div className="pager-buttons">
          <button
            type="button"
            className="pager-btn"
            onClick={() => onPage(current - 1)}
            disabled={current === 0}
            aria-label="Previous page"
          >
            ‹
          </button>
          {pages.map((p, i) => (
            <span key={p} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              {i > 0 && pages[i - 1] !== p - 1 ? <span className="faint">…</span> : null}
              <button
                type="button"
                className={p === current ? 'pager-btn current' : 'pager-btn'}
                onClick={() => onPage(p)}
                aria-current={p === current ? 'page' : undefined}
              >
                {p + 1}
              </button>
            </span>
          ))}
          <button
            type="button"
            className="pager-btn"
            onClick={() => onPage(current + 1)}
            disabled={current >= totalPages - 1}
            aria-label="Next page"
          >
            ›
          </button>
        </div>
      ) : null}
    </nav>
  )
}
