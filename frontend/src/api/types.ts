/**
 * TypeScript mirror of `docs/api-contract.md` — the source of truth for the
 * HTTP API. Field names, shapes and enums follow the contract exactly.
 */

// ---------- Enums ----------

export type ActivityType = 'CARD' | 'PAYMENT' | 'CRYPTO'

export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'FAILED' | 'REVERSED'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type FindingSeverity = 'LOW' | 'MEDIUM' | 'HIGH'

export type AnalysisStatus = 'COMPLETED' | 'FAILED'

/** Rules apply to one activity type or to all of them ('ALL'). */
export type RuleAppliesTo = ActivityType | 'ALL'

// ---------- Auth ----------

export interface Operator {
  id: string
  username: string
  displayName: string
  /** Not enumerated by the contract; demo data uses OPERATOR / SUPERVISOR. */
  role: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
  operator: Operator
}

// ---------- Paging envelope ----------

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ---------- Customers ----------

export interface CustomerSummary {
  id: string
  customerNumber: string
  fullName: string
  email: string
  country: string
  /** Not enumerated by the contract; demo data uses BASIC / VERIFIED / ENHANCED. */
  kycLevel: string
  riskScore: number
  transactionCount: number
  /** null when the customer has no transactions. */
  lastActivityAt: string | null
}

export interface CustomerProfile {
  id: string
  customerNumber: string
  fullName: string
  email: string
  country: string
  kycLevel: string
  dateOfBirth: string
  onboardedAt: string
}

// ---------- Activity overview ----------

export interface CurrencyTotal {
  currency: string
  totalAmount: number
}

export interface TypeBreakdown {
  activityType: ActivityType
  count: number
  failedCount: number
  totalsByCurrency: CurrencyTotal[]
}

export interface StatusCount {
  status: TransactionStatus
  count: number
}

export interface MonthlyCount {
  /** `YYYY-MM` */
  month: string
  card: number
  payment: number
  crypto: number
}

export interface TriggeredRule {
  ruleId: string
  ruleName: string
  appliesTo: RuleAppliesTo
  timesTriggered: number
  totalContribution: number
  lastTriggeredAt: string
}

export interface CustomerOverview {
  riskScore: number
  windowFrom: string | null
  windowTo: string | null
  transactionCount: number
  byType: TypeBreakdown[]
  byStatus: StatusCount[]
  monthlyCounts: MonthlyCount[]
  triggeredRules: TriggeredRule[]
}

// ---------- Transactions ----------

export interface CardDetails {
  cardPan: string
  cardType: string
  merchantName: string
  mccCode: string
  cardPresent: boolean
  authorizationCode: string | null
  declineReason: string | null
}

export interface PaymentDetails {
  paymentMethod: string
  senderAccount: string
  receiverAccount: string
  receiverBankCountry: string
}

export interface CryptoDetails {
  blockchain: string
  walletAddressFrom: string
  walletAddressTo: string
  txHash: string
  exchangeName: string | null
}

export interface Transaction {
  id: string
  activityType: ActivityType
  amount: number
  currency: string
  status: TransactionStatus
  createdAt: string
  riskScore: number
  triggeredRules: string[]
  /** Exactly one of card / payment / crypto is non-null, matching activityType. */
  card: CardDetails | null
  payment: PaymentDetails | null
  crypto: CryptoDetails | null
}

// ---------- AI analyses ----------

export interface Finding {
  title: string
  severity: FindingSeverity
  detail: string
  relatedTransactionIds: string[]
}

export interface CitedPolicy {
  chunkId: string
  documentTitle: string
  sectionTitle: string
  excerpt: string
}

/** List view (`GET /api/customers/{id}/analyses`). */
export interface AnalysisSummary {
  id: string
  customerId: string
  requestedBy: Operator
  requestedAt: string
  completedAt: string | null
  status: AnalysisStatus
  model: string
  /** null for FAILED analyses. */
  riskLevel: RiskLevel | null
  /** null for FAILED analyses. */
  summary: string | null
  transactionCount: number
  /** Set only for FAILED analyses. */
  errorMessage: string | null
}

/** Detail view (`GET /api/analyses/{analysisId}`, `POST .../analyses`). */
export interface AnalysisDetail extends AnalysisSummary {
  /** null/empty for FAILED analyses. */
  findings: Finding[] | null
  recommendations: string[] | null
  citedPolicies: CitedPolicy[] | null
  activityWindowFrom: string | null
  activityWindowTo: string | null
}

// ---------- Errors (RFC 7807) ----------

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
}
