import { useContext } from 'react'
import { AuthContext } from '../AuthProvider.tsx'

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === null) {
    throw Error('useAuth must be used within a AuthProvider')
  }
  return context
}
