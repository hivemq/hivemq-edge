import { JWTPayload } from './types/jwt-payload.ts'

export const QUERY_KEYS = {
  BRIDGES: 'bridges',
  CONNECTION_STATUS: 'connection.status',
  PROTOCOLS: 'protocols',
  ADAPTERS: 'adapters',
  UNIFIED_NAMESPACE: 'unified.namespace',
  GATEWAY: 'gateway',
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
