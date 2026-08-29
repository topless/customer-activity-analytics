import type { FindingSeverity, RiskLevel } from '../api/types'

/** Shared risk color band, used for numeric scores, risk levels and severities. */
export type RiskBand = 'none' | 'low' | 'medium' | 'high' | 'critical'

/** 0 → neutral; 1–49 green; 50–149 amber; 150–399 orange; ≥400 red. */
export function scoreBand(score: number): RiskBand {
  if (score <= 0) return 'none'
  if (score < 50) return 'low'
  if (score < 150) return 'medium'
  if (score < 400) return 'high'
  return 'critical'
}

export function levelBand(level: RiskLevel): RiskBand {
  switch (level) {
    case 'LOW':
      return 'low'
    case 'MEDIUM':
      return 'medium'
    case 'HIGH':
      return 'high'
    case 'CRITICAL':
      return 'critical'
  }
}

export function severityBand(severity: FindingSeverity): RiskBand {
  switch (severity) {
    case 'LOW':
      return 'low'
    case 'MEDIUM':
      return 'medium'
    case 'HIGH':
      return 'high'
  }
}
