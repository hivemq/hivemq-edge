import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { processToken } from '@/modules/Auth/auth-utilities.ts'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/utils.ts', async () => {
  const actual = await vi.importActual<object>('@/api/utils.ts')
  return {
    ...actual,
    verifyJWT: vi.fn(() => {
      return true
    }),
  }
})

describe('processToken', () => {
  const mockSetAuthToken = vi.fn()
  const mockSetLoading = vi.fn()
  const mockLogin = vi.fn(() => mockSetLoading(false))

  it('should accept a valid token', async () => {
    processToken(MOCK_JWT, mockSetAuthToken, mockLogin, mockSetLoading)
    expect(mockSetAuthToken).not.toHaveBeenCalled()
    expect(mockLogin).toHaveBeenCalledWith({ token: MOCK_JWT }, expect.anything())

    expect(mockSetLoading).toHaveBeenCalledWith(false)

    vi.restoreAllMocks()
  })
})
