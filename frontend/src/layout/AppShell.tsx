import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/context'
import { LoadingBlock } from '../components/ui'

function BrandMark() {
  return (
    <svg className="brand-mark" width="22" height="22" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="7" fill="rgba(255,255,255,0.07)" />
      <rect x="7" y="17" width="4.5" height="8" rx="1.2" fill="#5b82ec" />
      <rect x="13.75" y="12" width="4.5" height="13" rx="1.2" fill="#2fb6c2" />
      <rect x="20.5" y="7" width="4.5" height="18" rx="1.2" fill="#a06bd8" />
    </svg>
  )
}

/** Authenticated frame: dark top bar + routed page content. Redirects to /login otherwise. */
export function AppShell() {
  const { status, operator, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  if (status === 'restoring') {
    return <LoadingBlock label="Restoring session…" />
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  const role = operator?.role ?? ''

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="brand">
            <BrandMark />
            <span className="brand-name">
              Customer Activity <span className="thin">Analytics</span>
            </span>
          </Link>
          <div className="topbar-user">
            <span className="topbar-name">{operator?.displayName}</span>
            <span className={role === 'SUPERVISOR' ? 'role-badge supervisor' : 'role-badge'}>
              {role}
            </span>
            <button
              type="button"
              className="btn-logout"
              onClick={() => {
                logout()
                navigate('/login')
              }}
            >
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
