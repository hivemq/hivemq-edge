import { Dispatch, SetStateAction } from 'react'

import { ApiBearerToken } from '@/api/__generated__'
import { parseJWT, verifyJWT } from '@/api/utils.ts'

export const processToken = (
  authToken: string | undefined,
  setAuthToken: Dispatch<SetStateAction<string | undefined>>,
  login: (newUser: ApiBearerToken, callback: VoidFunction) => void,
  setLoading: Dispatch<SetStateAction<boolean>>
) => {
  if (!authToken) {
    setAuthToken(undefined)
    setLoading(false)
    return
  }

  const parsedToken = parseJWT(authToken)
  if (!parsedToken) {
    setAuthToken(undefined)
    setLoading(false)
    return
  }

  const isValid = verifyJWT(parsedToken)
  if (!isValid) {
    setAuthToken(undefined)
    setLoading(false)
    return
  }

  login({ token: authToken }, () => {
    setLoading(false)
  })
}

export const authUtilities = {
  isAuthenticated: false,
  login(callback: VoidFunction) {
    authUtilities.isAuthenticated = true
    setTimeout(callback, 1) // fake async
  },
  logout(callback: VoidFunction) {
    authUtilities.isAuthenticated = false
    setTimeout(callback, 1)
  },
}
