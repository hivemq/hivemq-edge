import { createContext, FunctionComponent, PropsWithChildren, useState } from 'react'
import { fakeAuthProvider } from './fakeAuthProvider'
import { ApiBearerToken } from '../../api/__generated__'

interface AuthContextType {
  credentials: ApiBearerToken | null
  isAuthenticated: boolean
  login: (user: ApiBearerToken, callback: VoidFunction) => void
  logout: (callback: VoidFunction) => void
}

export const AuthContext = createContext<AuthContextType | null>(null)

export const AuthProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
  const [credentials, setCredentials] = useState<ApiBearerToken | null>(null)

  const login = (newUser: ApiBearerToken, callback: VoidFunction) => {
    return fakeAuthProvider.login(() => {
      setCredentials(newUser)
      callback()
    })
  }

  const logout = (callback: VoidFunction) => {
    return fakeAuthProvider.logout(() => {
      setCredentials(null)
      callback()
    })
  }

  return (
    <AuthContext.Provider value={{ credentials, login, logout, isAuthenticated: fakeAuthProvider.isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  )
}
