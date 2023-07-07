import { createContext, FunctionComponent, PropsWithChildren, useEffect, useState } from 'react'

import { ApiBearerToken } from '@/api/__generated__'
import { useLocalStorage } from '@/hooks/useLocalStorage/useLocalStorage.ts'
import { authUtilities, processToken } from '@/modules/Auth/auth-utilities.ts'

interface AuthContextType {
  credentials: ApiBearerToken | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (user: ApiBearerToken, callback: VoidFunction) => void
  logout: (callback: VoidFunction) => void
}

export const AuthContext = createContext<AuthContextType | null>(null)

export const AuthProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
  const [credentials, setCredentials] = useState<ApiBearerToken | null>(null)

  const [isAuthToken, setAuthToken] = useLocalStorage<string | undefined>('auth', undefined)
  const [isLoading, setLoading] = useState(true)

  useEffect(() => {
    processToken(isAuthToken, setAuthToken, login, setLoading)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = (newUser: ApiBearerToken, callback: VoidFunction) => {
    return authUtilities.login(() => {
      setCredentials(newUser)
      setAuthToken(newUser.token)
      callback()
    })
  }

  const logout = (callback: VoidFunction) => {
    return authUtilities.logout(() => {
      setCredentials(null)
      setAuthToken(undefined)
      callback()
    })
  }

  return (
    <AuthContext.Provider
      value={{ credentials, login, logout, isLoading, isAuthenticated: authUtilities.isAuthenticated }}
    >
      {children}
    </AuthContext.Provider>
  )
}
