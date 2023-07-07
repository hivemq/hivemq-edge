import { describe, it, expect } from 'vitest'

import { MOCK_JWT } from '@/__test-utils__/mocks.ts'

import { parseJWT, verifyJWT } from './utils'
import { JWTPayload } from './types/jwt-payload.ts'

const MOCK_DURATION = 30 * 60
const MOCK_BEFORE = 120

const MOCK_EXPIRED: JWTPayload = {
  jti: 'OlJJYRU3r4fx8nJ0BygfSQ',
  iat: 1688555703,
  aud: 'HiveMQ-Edge-Api',
  iss: 'HiveMQ-Edge',
  exp: 1688557503,
  nbf: 1688555583,
  sub: 'admin',
  roles: ['admin'],
}

const MOCK_LIVE = (date: Date): JWTPayload => {
  const time = Math.floor(date.getTime() / 1000)
  return {
    ...MOCK_EXPIRED,
    iat: time,
    exp: time + MOCK_DURATION,
    nbf: time - MOCK_BEFORE,
  }
}

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

describe('verifyJWT', () => {
  it('should return false if token is not valid', () => {
    expect(verifyJWT(null)).toBe(false)
  })

  it('should return false if token has expired', () => {
    expect(verifyJWT(MOCK_EXPIRED)).toBe(false)
  })

  it('should return false if token has also expired', () => {
    const now = new Date(Date.now() - 1000 * (MOCK_DURATION + 60))
    expect(verifyJWT(MOCK_LIVE(now))).toBe(false)
  })

  it('should return true if token has not expired', () => {
    const now = new Date(Date.now() - 1000 * (MOCK_DURATION - 60))
    expect(verifyJWT(MOCK_LIVE(now))).toBe(true)
  })
})
