import type { NetworkMode } from '@tanstack/react-query'

interface configType {
  environment: string
  apiBaseUrl: string

  isDevMode: boolean
  isProdMode: boolean

  version: string
  documentationUrl: string

  httpClient: {
    axiosTimeout: number
    networkMode: NetworkMode
    pollingRefetchInterval: number
  }

  features: {
    DEV_MOCK_SERVER: boolean
    DATAHUB_FSM_REACT_FLOW: boolean
    WORKSPACE_EXPERIMENTAL: boolean
  }

  documentation: {
    namespace: string
  }
}

function getApiBaseUrl(): string {
  const url = window.location.href
  const index = url.lastIndexOf('/app/')
  if (index === -1) {
    return url
  }
  return url.substring(0, index)
}

const config: configType = {
  environment: import.meta.env.MODE,

  isDevMode: import.meta.env.MODE === 'development',
  isProdMode: import.meta.env.MODE === 'production',

  apiBaseUrl: getApiBaseUrl(),

  version: import.meta.env.VITE_APP_VERSION,
  documentationUrl: import.meta.env.VITE_APP_DOCUMENTATION,

  httpClient: {
    /**
     * Number of milliseconds before Axios times out the request
     * @see  https://axios-http.com/docs/req_config
     */
    axiosTimeout: 15000,

    /**
     * React Query network mode
     * @see https://tanstack.com/query/v5/docs/react/guides/network-mode
     */
    networkMode: 'always',

    /**
     * https://tanstack.com/query/v5/docs/react/reference/useQuery
     * @see https://tanstack.com/query/v5/docs/react/reference/useQuery
     */
    pollingRefetchInterval: 5 * 1000,
  },

  features: {
    /**
     * DEV-only. Runs the mock server, including the MQTT client-based sampling
     */
    DEV_MOCK_SERVER: import.meta.env.VITE_FLAG_MOCK_SERVER === 'true',

    /**
     * Visualise FSM using React Flow. If false, use Mermaid
     */
    DATAHUB_FSM_REACT_FLOW: import.meta.env.VITE_FLAG_DATAHUB_FSM_REACTFLOW === 'true',

    WORKSPACE_EXPERIMENTAL: import.meta.env.VITE_FLAG_WORKSPACE_SHOW_EXPERIMENTAL === 'true',
  },

  documentation: {
    // TODO[NVL] Is this the right place?
    namespace: 'https://www.hivemq.com/solutions/manufacturing/smart-manufacturing-using-isa95-mqtt-sparkplug-and-uns/',
  },
}

export default config
