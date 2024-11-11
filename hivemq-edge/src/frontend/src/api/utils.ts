import { JWTPayload } from './types/jwt-payload.ts'

export const QUERY_KEYS = {
  BRIDGES: 'bridges',
  CONNECTION_STATUS: 'connection.status',
  DISCOVERY_POINTS: 'discovery.points',
  DISCOVERY_SCHEMAS: 'discovery.schemas',
  DISCOVERY_TOPICS: 'discovery.topics',
  DISCOVERY_TOPIC_FILTERS: 'discovery.topic-filters',
  DISCOVERY_TAGS: 'discovery.tags',
  DISCOVERY_PAYLOADS: 'discovery.payloads',
  PROTOCOLS: 'protocols',
  ADAPTERS: 'adapters',
  CLIENTS: 'broker-clients',
  UNIFIED_NAMESPACE: 'unified.namespace',
  FRONTEND_CONFIGURATION: 'frontend.configuration',
  FRONTEND_NOTIFICATION: 'frontend.notification',
  FRONTEND_CAPABILITIES: 'frontend.capabilities',
  LISTENERS: 'gateway.listeners',
  METRICS: 'metrics',
  METRICS_SAMPLE: 'sample',
  EVENTS: 'events',
  GITHUB_RELEASES: 'github.releases',
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
