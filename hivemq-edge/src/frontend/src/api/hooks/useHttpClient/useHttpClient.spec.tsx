import { MemoryRouter } from 'react-router-dom'
import { RequestHandler, rest } from 'msw'
import { vi, expect, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'

import config from '@/config'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'

import { AxiosHttpRequestWithInterceptors, useHttpClient } from './useHttpClient.ts'
import { ApiBearerToken, ApiError } from '@/api/__generated__'

enum auth {
  HEADER_ATTACHED = 'HEADER_ATTACHED',
  HEADER_MISSING = 'HEADER_MISSING',
}

interface MockAuthContextType {
  credentials: ApiBearerToken | undefined
  isAuthenticated?: boolean
  isLoading?: boolean
  login?: (user: ApiBearerToken, callback: VoidFunction) => void
  logout?: (callback: VoidFunction) => void
}

const { mockedMethod } = vi.hoisted(() => {
  return {
    mockedMethod: vi.fn<[], MockAuthContextType>(() => ({ credentials: { token: 'fake token' } })),
  }
})

vi.mock('@/modules/Auth/hooks/useAuth.ts', () => ({ useAuth: mockedMethod }))

const successURL = '/my-successful-endpoint'
const errorURL = '/my-error-endpoint'
const reissueURL = '/my-reissue-endpoint'
const handlers: RequestHandler[] = [
  rest.get(`${config.apiBaseUrl}${successURL}`, (req, res, ctx) => {
    let bodyContent = auth.HEADER_MISSING
    const authHeader = req.headers.get('authorization') ?? null
    if (authHeader?.startsWith('Bearer ')) {
      const token = authHeader.substring(7, authHeader.length)
      if (token) {
        bodyContent = auth.HEADER_ATTACHED
      }
    }

    return res(
      ctx.status(200),
      ctx.json({
        bodyContent: bodyContent,
      })
    )
  }),

  rest.get(`${config.apiBaseUrl}${errorURL}`, (_, res, ctx) => {
    return res(
      ctx.status(401),
      ctx.json({
        bodyContent: 'an error message',
      })
    )
  }),

  rest.get(`${config.apiBaseUrl}${reissueURL}`, (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.set({
        'X-Bearer-Token-Reissue': 'a-new-token',
      }),
      ctx.json({
        bodyContent: 'A new token has been added to the response',
      })
    )
  }),
]

const wrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <AuthProvider>
    <MemoryRouter>{children}</MemoryRouter>
  </AuthProvider>
)

describe('useSpringClient', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should attach a jwt token to the authorization header', async () => {
    server.use(...handlers)
    mockedMethod.mockReturnValue({
      credentials: { token: 'one' },
    })

    const { result } = renderHook(useHttpClient, {
      wrapper,
    })
    const currentClient = result.current.request as AxiosHttpRequestWithInterceptors

    const serverResponse = await currentClient.request<string>({
      method: 'GET',
      url: successURL,
    })

    expect(serverResponse).toStrictEqual({ bodyContent: auth.HEADER_ATTACHED })
  })

  it('should not have a token if not authenticated', async () => {
    server.use(...handlers)
    mockedMethod.mockReturnValue({
      credentials: { token: undefined },
    })

    const { result } = renderHook(useHttpClient, {
      wrapper,
    })
    const currentClient = result.current.request as AxiosHttpRequestWithInterceptors

    const serverResponse = await currentClient.request<string>({
      method: 'GET',
      url: successURL,
    })

    expect(serverResponse).toStrictEqual({ bodyContent: auth.HEADER_MISSING })
  })

  it('should logout when 401 is returned ', async () => {
    const mockLogout = vi.fn()
    server.use(...handlers)
    mockedMethod.mockReturnValue({
      credentials: { token: undefined },
      logout: mockLogout,
    })

    const { result } = renderHook(useHttpClient, {
      wrapper,
    })
    const currentClient = result.current.request as AxiosHttpRequestWithInterceptors

    await currentClient
      .request<string>({
        method: 'GET',
        url: errorURL,
      })
      .catch((err: ApiError) => {
        expect(err).toHaveProperty('status', 401)
        expect(err).toHaveProperty('statusText', 'Unauthorized')
        expect(err).toHaveProperty('body.bodyContent', 'an error message')
        expect(mockLogout).toHaveBeenCalledOnce()
      })
  })

  it.skip("should change the JWT credentials when a 'x-bearer-token-reissue' is intercepted", async () => {
    const mockLogout = vi.fn()
    server.use(...handlers)
    mockedMethod.mockReturnValue({
      credentials: { token: undefined },
      logout: mockLogout,
    })

    const { result } = renderHook(useHttpClient, {
      wrapper,
    })
    const currentClient = result.current.request as AxiosHttpRequestWithInterceptors
    await currentClient.request<string>({
      method: 'GET',
      url: successURL,
    })
  })
})
