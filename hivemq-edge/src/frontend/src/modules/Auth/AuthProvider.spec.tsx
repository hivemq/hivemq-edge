import { useContext } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, waitFor } from '@testing-library/react'

import { MOCK_JWT } from '@/__test-utils__/mocks.ts'

import { AuthContext, AuthProvider } from './AuthProvider.tsx'

const loginCallback = vi.fn()
const logoutCallback = vi.fn()

const TestingComponent = () => {
  const context = useContext(AuthContext)
  return (
    <>
      <p data-testid="credentials">{context?.credentials?.token}</p>
      <p data-testid="isLoading">{context?.isLoading ? 'true' : 'false'}</p>
      <p data-testid="isAuthenticated">{context?.isLoading ? 'true' : 'false'}</p>
      <button
        data-testid="login"
        onClick={() => {
          context?.login({ token: MOCK_JWT }, loginCallback)
        }}
      >
        Login
      </button>
      <button
        data-testid="logout"
        onClick={() => {
          context?.logout(logoutCallback)
        }}
      >
        Login
      </button>
    </>
  )
}

describe('AuthProvider', () => {
  it('should return false if token has expired', async () => {
    const { getByTestId } = render(
      <AuthProvider>
        <TestingComponent />
      </AuthProvider>
    )
    expect(getByTestId('credentials').textContent).toEqual('')

    expect(getByTestId('isLoading').textContent).toEqual('false')
    expect(getByTestId('isAuthenticated').textContent).toEqual('false')
    getByTestId('login').click()

    await waitFor(() => {
      expect(loginCallback).toHaveBeenCalledOnce()
      expect(getByTestId('credentials').textContent).toEqual(MOCK_JWT)
    })

    getByTestId('logout').click()
    await waitFor(() => {
      expect(logoutCallback).toHaveBeenCalledOnce()
      expect(getByTestId('credentials').textContent).toEqual('')
    })
  })
})
