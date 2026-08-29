import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearToken, getToken, setToken } from '../api/client'
import type { Operator } from '../api/types'
import { AuthContext, type AuthStatus } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>(() => (getToken() ? 'restoring' : 'anonymous'))
  const [operator, setOperator] = useState<Operator | null>(null)

  // Restore the session on app load: a stored token is validated via /api/auth/me.
  useEffect(() => {
    if (!getToken()) return
    const controller = new AbortController()
    api
      .me(controller.signal)
      .then((me) => {
        setOperator(me)
        setStatus('authenticated')
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        clearToken()
        setOperator(null)
        setStatus('anonymous')
      })
    return () => controller.abort()
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const response = await api.login(username, password)
    setToken(response.token)
    setOperator(response.operator)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setOperator(null)
    setStatus('anonymous')
  }, [])

  const value = useMemo(
    () => ({ status, operator, login, logout }),
    [status, operator, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
