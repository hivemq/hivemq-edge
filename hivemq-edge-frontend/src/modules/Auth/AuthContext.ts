import { createContext } from 'react'
import type { ApiBearerToken } from '@/api/__generated__'

interface AuthContextType {
  credentials: ApiBearerToken | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (user: ApiBearerToken, callback: VoidFunction) => void
  logout: (callback: VoidFunction) => void
}

export const AuthContext = createContext<AuthContextType | null>(null)
