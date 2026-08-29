import { createContext, useContext } from 'react'
import type { Operator } from '../api/types'

export type AuthStatus = 'restoring' | 'anonymous' | 'authenticated'

export interface AuthContextValue {
  status: AuthStatus
  operator: Operator | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within <AuthProvider>')
  return context
}
