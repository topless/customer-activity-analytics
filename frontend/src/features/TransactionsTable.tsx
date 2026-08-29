import { Fragment, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { ActivityType, Page, Transaction, TransactionStatus } from '../api/types'
import { RiskScoreBadge, StatusChip, TypeChip } from '../components/badges'
import { EmptyState, ErrorAlert, LoadingBlock, Pager } from '../components/ui'
import { formatAmount, formatDateTime, middleEllipsis } from '../lib/format'
import './transactions.css'

const PAGE_SIZE = 25

const TYPE_OPTIONS: ActivityType[] = ['CARD', 'PAYMENT', 'CRYPTO']
const STATUS_OPTIONS: TransactionStatus[] = ['COMPLETED', 'PENDING', 'FAILED', 'REVERSED']

/** One-line context shown in the collapsed row, per activity type. */
function contextSummary(tx: Transaction): string {
  if (tx.card) return tx.card.merchantName
  if (tx.payment) return `${tx.payment.paymentMethod} → ${tx.payment.receiverBankCountry}`
  if (tx.crypto) {
    return tx.crypto.exchangeName
      ? `${tx.crypto.blockchain} · ${tx.crypto.exchangeName}`
      : tx.crypto.blockchain
  }
  return ''
}

function DetailItem({
  label,
  value,
  mono = false,
  negative = false,
  title,
}: {
  label: string
  value: string
  mono?: boolean
  negative?: boolean
  title?: string
}) {
  const classes = [mono ? 'mono' : '', negative ? 'negative' : ''].filter(Boolean).join(' ')
  return (
    <div className="tx-detail-item">
      <dt>{label}</dt>
      <dd className={classes || undefined} title={title}>
        {value}
      </dd>
    </div>
  )
}

function TransactionDetails({ tx }: { tx: Transaction }) {
  return (
    <>
      <dl className="tx-details">
        {tx.card ? (
          <>
            <DetailItem label="Merchant" value={tx.card.merchantName} />
            <DetailItem label="MCC" value={tx.card.mccCode} mono />
            <DetailItem label="PAN" value={tx.card.cardPan} mono />
            <DetailItem label="Card type" value={tx.card.cardType} />
            <DetailItem label="Card present" value={tx.card.cardPresent ? 'Yes' : 'No'} />
            <DetailItem label="Auth code" value={tx.card.authorizationCode ?? '—'} mono />
            {tx.card.declineReason ? (
              <DetailItem label="Decline reason" value={tx.card.declineReason} negative />
            ) : null}
          </>
        ) : null}
        {tx.payment ? (
          <>
            <DetailItem label="Method" value={tx.payment.paymentMethod} />
            <DetailItem label="Sender account" value={tx.payment.senderAccount} mono />
            <DetailItem label="Receiver account" value={tx.payment.receiverAccount} mono />
            <DetailItem label="Receiver country" value={tx.payment.receiverBankCountry} />
          </>
        ) : null}
        {tx.crypto ? (
          <>
            <DetailItem label="Blockchain" value={tx.crypto.blockchain} />
            <DetailItem
              label="From wallet"
              value={middleEllipsis(tx.crypto.walletAddressFrom)}
              title={tx.crypto.walletAddressFrom}
              mono
            />
            <DetailItem
              label="To wallet"
              value={middleEllipsis(tx.crypto.walletAddressTo)}
              title={tx.crypto.walletAddressTo}
              mono
            />
            <DetailItem
              label="Tx hash"
              value={middleEllipsis(tx.crypto.txHash, 12, 8)}
              title={tx.crypto.txHash}
              mono
            />
            <DetailItem label="Exchange" value={tx.crypto.exchangeName ?? '— (unhosted)'} />
          </>
        ) : null}
      </dl>
      {tx.triggeredRules.length > 0 ? (
        <div className="tx-detail-rules">
          <span>Triggered rules:</span>
          {tx.triggeredRules.map((rule) => (
            <span key={rule} className="chip band-high">
              {rule}
            </span>
          ))}
        </div>
      ) : null}
    </>
  )
}

export function TransactionsTable({ customerId }: { customerId: string }) {
  const [type, setType] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  // Results keyed by request parameters; loading/error derived from key matches.
  const requestKey = `${customerId}|${type}|${status}|${page}`
  const [result, setResult] = useState<{ key: string; page: Page<Transaction> } | null>(null)
  const [failure, setFailure] = useState<{ key: string; message: string } | null>(null)
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(new Set())

  useEffect(() => {
    const key = `${customerId}|${type}|${status}|${page}`
    const controller = new AbortController()
    api
      .getTransactions(
        customerId,
        { type: type || undefined, status: status || undefined, page, size: PAGE_SIZE },
        controller.signal,
      )
      .then((response) => {
        setResult({ key, page: response })
        setExpanded(new Set())
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setFailure({ key, message: errorMessage(cause) })
      })
    return () => controller.abort()
  }, [customerId, type, status, page])

  const data = result?.page ?? null
  const error = failure?.key === requestKey ? failure.message : null
  const loading = result?.key !== requestKey && error === null

  function toggleExpanded(id: string) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const hasFilters = type !== '' || status !== ''

  return (
    <section className="card" aria-label="Transactions">
      <div className="card-head">
        <h2 className="card-title">Transactions</h2>
        <div className="tx-toolbar">
          <label className="field-label" htmlFor="tx-type" style={{ fontSize: 11 }}>
            Type
          </label>
          <select
            id="tx-type"
            className="select"
            value={type}
            onChange={(e) => {
              setType(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All types</option>
            {TYPE_OPTIONS.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <label className="field-label" htmlFor="tx-status" style={{ fontSize: 11 }}>
            Status
          </label>
          <select
            id="tx-status"
            className="select"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error ? (
        <div className="card-body">
          <ErrorAlert message={error} />
        </div>
      ) : data === null ? (
        <LoadingBlock label="Loading transactions…" />
      ) : data.content.length === 0 ? (
        <EmptyState
          title={hasFilters ? 'No matching transactions' : 'No transactions'}
          hint={
            hasFilters
              ? 'No transactions match the selected filters. Try clearing them.'
              : 'This customer has no recorded activity yet.'
          }
        />
      ) : (
        <>
          <div className="table-wrap">
            <table className={loading ? 'table table-loading' : 'table'}>
              <thead>
                <tr>
                  <th style={{ width: 34 }} aria-label="Expand" />
                  <th>Date</th>
                  <th>Type</th>
                  <th>Details</th>
                  <th className="th-num">Amount</th>
                  <th>Status</th>
                  <th className="th-num">Risk</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((tx) => {
                  const isOpen = expanded.has(tx.id)
                  return (
                    <Fragment key={tx.id}>
                      <tr>
                        <td>
                          <button
                            type="button"
                            className="tx-expand-btn"
                            onClick={() => toggleExpanded(tx.id)}
                            aria-expanded={isOpen}
                            aria-label={isOpen ? 'Collapse details' : 'Expand details'}
                          >
                            <svg width="11" height="11" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                              <path
                                d="m4 2 4 4-4 4"
                                stroke="currentColor"
                                strokeWidth="1.7"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                              />
                            </svg>
                          </button>
                        </td>
                        <td className="nowrap">{formatDateTime(tx.createdAt)}</td>
                        <td>
                          <TypeChip type={tx.activityType} />
                        </td>
                        <td>
                          <div className="tx-context" title={contextSummary(tx)}>
                            {contextSummary(tx)}
                          </div>
                        </td>
                        <td className="td-num">
                          <span className="tx-amount">
                            {formatAmount(tx.amount)}
                            <span className="currency">{tx.currency}</span>
                          </span>
                        </td>
                        <td>
                          <StatusChip status={tx.status} />
                        </td>
                        <td className="td-num">
                          {tx.riskScore > 0 ? <RiskScoreBadge score={tx.riskScore} /> : null}
                        </td>
                      </tr>
                      {isOpen ? (
                        <tr className="tx-detail-row">
                          <td colSpan={7}>
                            <TransactionDetails tx={tx} />
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  )
                })}
              </tbody>
            </table>
          </div>
          <Pager page={data} onPage={setPage} />
        </>
      )}
    </section>
  )
}
