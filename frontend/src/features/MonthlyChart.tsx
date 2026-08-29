import { useLayoutEffect, useRef, useState } from 'react'
import type { MonthlyCount } from '../api/types'
import { formatMonth } from '../lib/format'
import './chart.css'

const SERIES = [
  { key: 'card', label: 'Card', color: 'var(--series-card)' },
  { key: 'payment', label: 'Payment', color: 'var(--series-payment)' },
  { key: 'crypto', label: 'Crypto', color: 'var(--series-crypto)' },
] as const

const HEIGHT = 248
const MARGIN = { top: 12, right: 10, bottom: 28, left: 42 }

/** Round up to a "nice" axis maximum and return its tick values (integers — counts). */
function niceScale(maxValue: number): { max: number; ticks: number[] } {
  const target = Math.max(maxValue, 1)
  const rough = target / 4
  const magnitude = 10 ** Math.floor(Math.log10(rough))
  let step = magnitude
  for (const factor of [1, 2, 5, 10]) {
    if (rough <= factor * magnitude) {
      step = factor * magnitude
      break
    }
  }
  step = Math.max(1, Math.round(step))
  const max = Math.ceil(target / step) * step
  const ticks: number[] = []
  for (let v = 0; v <= max + step / 2; v += step) ticks.push(v)
  return { max, ticks }
}

function useContainerWidth(): [React.RefObject<HTMLDivElement | null>, number] {
  const ref = useRef<HTMLDivElement | null>(null)
  const [width, setWidth] = useState(0)

  useLayoutEffect(() => {
    const element = ref.current
    if (!element) return
    const update = () => setWidth(element.clientWidth)
    update()
    const observer = new ResizeObserver(update)
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  return [ref, width]
}

export function ChartLegend() {
  return (
    <div className="chart-legend" aria-hidden="true">
      {SERIES.map((s) => (
        <span key={s.key} className="chart-legend-item">
          <span className="swatch" style={{ background: s.color }} />
          {s.label}
        </span>
      ))}
    </div>
  )
}

/** Hand-rolled SVG grouped bar chart of monthly card / payment / crypto counts. */
export function MonthlyChart({ months }: { months: MonthlyCount[] }) {
  const [containerRef, width] = useContainerWidth()
  const [hovered, setHovered] = useState<number | null>(null)

  if (months.length === 0) {
    return (
      <div className="chart-wrap">
        <div className="loading-block" style={{ padding: '56px 0' }}>
          No activity in the reporting window.
        </div>
      </div>
    )
  }

  const plotWidth = Math.max(width - MARGIN.left - MARGIN.right, 0)
  const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom
  const baseline = MARGIN.top + plotHeight

  const rawMax = Math.max(...months.flatMap((m) => [m.card, m.payment, m.crypto]))
  const { max, ticks } = niceScale(rawMax)
  const y = (value: number) => baseline - (value / max) * plotHeight

  const band = months.length > 0 ? plotWidth / months.length : 0
  const groupWidth = Math.min(band * 0.66, 64)
  const barGap = 2
  const barWidth = Math.max((groupWidth - 2 * barGap) / 3, 2)
  const groupX = (i: number) => MARGIN.left + i * band + (band - groupWidth) / 2

  // Thin out x labels when bands get narrow.
  const labelEvery = band >= 36 ? 1 : band >= 20 ? 2 : 3

  const hoveredMonth = hovered !== null ? months[hovered] : undefined
  const hoveredTop =
    hoveredMonth !== undefined
      ? y(Math.max(hoveredMonth.card, hoveredMonth.payment, hoveredMonth.crypto))
      : 0

  return (
    <div className="chart-wrap" ref={containerRef}>
      {width > 0 ? (
        <svg
          width={width}
          height={HEIGHT}
          role="img"
          aria-label="Monthly activity: card, payment and crypto transaction counts per month"
          onMouseLeave={() => setHovered(null)}
        >
          {/* Gridlines + y-axis labels */}
          {ticks.map((tick) => (
            <g key={tick}>
              <line
                x1={MARGIN.left}
                x2={width - MARGIN.right}
                y1={y(tick)}
                y2={y(tick)}
                stroke={tick === 0 ? 'var(--line-strong)' : 'var(--line)'}
                strokeWidth={1}
                shapeRendering="crispEdges"
              />
              <text
                x={MARGIN.left - 8}
                y={y(tick) + 3.5}
                textAnchor="end"
                fontSize={11}
                fill="var(--ink-faint)"
                className="num"
              >
                {tick}
              </text>
            </g>
          ))}

          {/* Bars */}
          {months.map((month, i) => {
            const x0 = groupX(i)
            const isJanuaryOrFirst = i === 0 || month.month.endsWith('-01')
            return (
              <g key={month.month}>
                {hovered === i ? (
                  <rect
                    x={MARGIN.left + i * band}
                    y={MARGIN.top}
                    width={band}
                    height={plotHeight}
                    fill="rgba(37, 84, 199, 0.055)"
                    rx={4}
                  />
                ) : null}
                {SERIES.map((series, s) => {
                  const value = month[series.key]
                  if (value <= 0) return null
                  const top = y(value)
                  return (
                    <rect
                      key={series.key}
                      className="chart-bar"
                      style={{ animationDelay: `${i * 22}ms` }}
                      x={x0 + s * (barWidth + barGap)}
                      y={top}
                      width={barWidth}
                      height={baseline - top}
                      rx={1.5}
                      fill={series.color}
                    >
                      {/* value access without a pointer (screen readers, native tooltip) */}
                      <title>{`${formatMonth(month.month, true)}: ${value} ${series.label.toLowerCase()}`}</title>
                    </rect>
                  )
                })}
                {i % labelEvery === 0 ? (
                  <text
                    x={MARGIN.left + i * band + band / 2}
                    y={HEIGHT - 8}
                    textAnchor="middle"
                    fontSize={11}
                    fill="var(--ink-soft)"
                  >
                    {formatMonth(month.month, isJanuaryOrFirst)}
                  </text>
                ) : null}
                {/* Invisible hover target covering the whole band */}
                <rect
                  x={MARGIN.left + i * band}
                  y={MARGIN.top}
                  width={band}
                  height={plotHeight}
                  fill="transparent"
                  onMouseEnter={() => setHovered(i)}
                />
              </g>
            )
          })}
        </svg>
      ) : null}

      {hoveredMonth !== undefined && hovered !== null ? (
        <div
          className="chart-tooltip"
          style={{
            // Offsets include the .chart-wrap padding (12px left, 6px top).
            left: MARGIN.left + hovered * band + band / 2 + 12,
            top: Math.max(hoveredTop, MARGIN.top) - 2,
          }}
        >
          <div className="chart-tooltip-title">{formatMonth(hoveredMonth.month, true)}</div>
          {SERIES.map((series) => (
            <div key={series.key} className="chart-tooltip-row">
              <span className="swatch" style={{ background: series.color }} />
              {series.label}
              <span className="value">{hoveredMonth[series.key]}</span>
            </div>
          ))}
          <div className="chart-tooltip-row chart-tooltip-total">
            Total
            <span className="value">
              {hoveredMonth.card + hoveredMonth.payment + hoveredMonth.crypto}
            </span>
          </div>
        </div>
      ) : null}
    </div>
  )
}
