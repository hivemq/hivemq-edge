import { describe, it, expect } from 'vitest'
import { parseJWT } from './utils'
import { JWTPayload } from './types/jwt-payload.ts'

const MOCK_JWT =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
  'eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.' +
  'SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c'

describe('parseJwt', () => {
  it('should parse a valid JWT token', () => {
    expect(parseJWT(MOCK_JWT)).toStrictEqual<JWTPayload>({
      iat: 1516239022,
      name: 'John Doe',
      sub: '1234567890',
    })
  })

  it('should return null when the token is not valid', () => {
    expect(parseJWT('A DUMMY TOKEN')).toBe(null)
  })
})
