import { useContext } from 'react'
import { AuthContext } from '@/modules/Auth/AuthContext.ts'

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === null) {
    throw new Error('useAuth must be used within a AuthProvider')
  }
  return context
}
