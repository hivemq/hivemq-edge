import { JWTPayload } from './types/jwt-payload.ts'

export const QUERY_KEYS = {
  BRIDGES: 'bridges',
  CONNECTION_STATUS: 'connection.status',
  PROTOCOLS: 'protocols',
  ADAPTERS: 'adapters',
  UNIFIED_NAMESPACE: 'unified.namespace',
  FRONTEND_CONFIGURATION: 'frontend.configuration',
  LISTENERS: 'gateway.listeners',
  METRICS: 'metrics',
  METRICS_SAMPLE: 'sample',
}

export const parseJWT = (token: string): JWTPayload | null => {
  try {
    return JSON.parse(atob(token.split('.')[1])) as JWTPayload
  } catch (e) {
    return null
  }
}

export const verifyJWT = (parsedToken: JWTPayload | null) => {
  if (!parsedToken) return false

  if (!parsedToken.exp) return false
  return parsedToken.exp * 1000 >= Date.now()
}
