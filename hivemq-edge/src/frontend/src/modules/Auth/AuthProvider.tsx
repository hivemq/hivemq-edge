import { createContext, FunctionComponent, PropsWithChildren, useEffect, useState } from 'react'

import { parseJWT, verifyJWT } from '@/api/utils.ts'
import { ApiBearerToken } from '@/api/__generated__'
import { useLocalStorage } from '@/hooks/useLocalStorage/useLocalStorage.ts'

import { fakeAuthProvider } from './fakeAuthProvider'

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
    if (isAuthToken) {
      const parsedToken = parseJWT(isAuthToken)
      if (parsedToken) {
        const isValid = verifyJWT(parsedToken)
        if (isValid) {
          login({ token: isAuthToken }, () => {
            setLoading(false)
          })
        } else {
          setAuthToken(undefined)
          setLoading(false)
        }
      }
    } else {
      setLoading(false)
    }
  }, [])

  const login = (newUser: ApiBearerToken, callback: VoidFunction) => {
    return fakeAuthProvider.login(() => {
      setCredentials(newUser)
      callback()
    })
  }

  const logout = (callback: VoidFunction) => {
    return fakeAuthProvider.logout(() => {
      setCredentials(null)
      setAuthToken(undefined)
      callback()
    })
  }

  return (
    <AuthContext.Provider
      value={{ credentials, login, logout, isLoading, isAuthenticated: fakeAuthProvider.isAuthenticated }}
    >
      {children}
    </AuthContext.Provider>
  )
}
