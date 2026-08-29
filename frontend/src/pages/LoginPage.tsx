import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/context'
import { ErrorAlert, Spinner } from '../components/ui'
import './login.css'

const DEMO_CREDENTIALS = [
  { username: 'alice', password: 'operator123', role: 'operator' },
  { username: 'bob', password: 'supervisor123', role: 'supervisor' },
]

export function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/'} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await login(username.trim(), password)
      // Redirect happens via the <Navigate> above once status flips.
    } catch (cause) {
      setError(errorMessage(cause))
      setSubmitting(false)
    }
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-band" aria-hidden="true" />
        <div className="login-body">
          <div className="login-brand">
            <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true">
              <rect width="32" height="32" rx="7" fill="#142033" />
              <rect x="7" y="17" width="4.5" height="8" rx="1.2" fill="#5b82ec" />
              <rect x="13.75" y="12" width="4.5" height="13" rx="1.2" fill="#2fb6c2" />
              <rect x="20.5" y="7" width="4.5" height="18" rx="1.2" fill="#a06bd8" />
            </svg>
            <span className="login-brand-name">
              Customer Activity
              <br />
              <span className="thin">Analytics</span>
            </span>
          </div>

          <div>
            <h1 className="login-title">Operator sign-in</h1>
            <p className="login-subtitle">Customer care &amp; risk review console</p>
          </div>

          {error ? <ErrorAlert message={error} /> : null}

          <form className="login-form" onSubmit={handleSubmit}>
            <div className="field">
              <label className="field-label" htmlFor="login-username">
                Username
              </label>
              <input
                id="login-username"
                className="input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                autoFocus
                required
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="login-password">
                Password
              </label>
              <input
                id="login-password"
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <Spinner /> : null}
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="login-hint">
            <div className="login-hint-title">Demo credentials</div>
            {DEMO_CREDENTIALS.map((cred) => (
              <button
                key={cred.username}
                type="button"
                className="login-cred"
                onClick={() => {
                  setUsername(cred.username)
                  setPassword(cred.password)
                }}
                title="Click to fill the form"
              >
                <span className="login-cred-user">{cred.username}</span>
                <span className="login-cred-pass">/ {cred.password}</span>
                <span className="faint">({cred.role})</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
