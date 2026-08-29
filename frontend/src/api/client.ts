import type {
  AnalysisDetail,
  AnalysisSummary,
  CustomerOverview,
  CustomerProfile,
  CustomerSummary,
  LoginResponse,
  Operator,
  Page,
  ProblemDetail,
  Transaction,
} from './types'

// JWT in localStorage: accepted XSS trade-off for this demo — an HttpOnly,
// SameSite cookie set by the backend would be the production choice.
const TOKEN_KEY = 'caa.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** Error thrown for any non-2xx response, carrying the RFC 7807 problem detail. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(message: string, status: number, problem: ProblemDetail | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
  signal?: AbortSignal
  /** Set false for the login call: no Bearer header, and a 401 means bad credentials. */
  auth?: boolean
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal, auth = true } = options

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    // No fetch timeout on purpose: POST .../analyses is synchronous and can
    // take up to ~2 minutes with a real LLM.
    response = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError('Network error — the server could not be reached.', 0)
  }

  if (response.status === 401 && auth) {
    // Session expired or token invalid: clear it and start over at /login.
    clearToken()
    window.location.assign('/login')
    throw new ApiError('Session expired — signing you out.', 401)
  }

  if (!response.ok) {
    let problem: ProblemDetail | null = null
    try {
      const contentType = response.headers.get('Content-Type') ?? ''
      if (contentType.includes('json')) problem = (await response.json()) as ProblemDetail
    } catch {
      // Non-JSON error body; fall through to the generic message.
    }
    const message =
      problem?.detail ?? problem?.title ?? `Request failed (HTTP ${response.status}).`
    throw new ApiError(message, response.status, problem)
  }

  return (await response.json()) as T
}

/** Human-readable message for any error thrown by the client. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Something went wrong.'
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const s = search.toString()
  return s ? `?${s}` : ''
}

// ---------- Typed endpoints (docs/api-contract.md) ----------

export const api = {
  login(username: string, password: string): Promise<LoginResponse> {
    return request('/api/auth/login', { method: 'POST', body: { username, password }, auth: false })
  },

  me(signal?: AbortSignal): Promise<Operator> {
    return request('/api/auth/me', { signal })
  },

  searchCustomers(
    params: { query?: string; page?: number; size?: number },
    signal?: AbortSignal,
  ): Promise<Page<CustomerSummary>> {
    return request(`/api/customers${query(params)}`, { signal })
  },

  getCustomer(id: string, signal?: AbortSignal): Promise<CustomerProfile> {
    return request(`/api/customers/${id}`, { signal })
  },

  getOverview(id: string, signal?: AbortSignal): Promise<CustomerOverview> {
    return request(`/api/customers/${id}/overview`, { signal })
  },

  getTransactions(
    id: string,
    params: { type?: string; status?: string; page?: number; size?: number },
    signal?: AbortSignal,
  ): Promise<Page<Transaction>> {
    return request(`/api/customers/${id}/transactions${query(params)}`, { signal })
  },

  runAnalysis(customerId: string): Promise<AnalysisDetail> {
    return request(`/api/customers/${customerId}/analyses`, { method: 'POST' })
  },

  listAnalyses(customerId: string, signal?: AbortSignal): Promise<AnalysisSummary[]> {
    return request(`/api/customers/${customerId}/analyses`, { signal })
  },

  getAnalysis(analysisId: string, signal?: AbortSignal): Promise<AnalysisDetail> {
    return request(`/api/analyses/${analysisId}`, { signal })
  },
}
