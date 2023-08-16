import { NetworkMode } from '@tanstack/react-query'

interface configType {
  environment: string
  apiBaseUrl: string

  version: string
  documentationUrl: string

  httpClient: {
    axiosTimeout: number
    networkMode: NetworkMode
    pollingRefetchInterval: number
  }

  features: {
    PROTOCOL_ADAPTER_FACET: boolean
    METRICS_SELECT_PANEL: boolean
    METRICS_DEFAULTS: string[]
  }

  documentation: {
    namespace: string
  }
}

const config: configType = {
  environment: import.meta.env.MODE,

  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,

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
     * Enable the workspace flow for the edge
     */
    WORKSPACE_FLOW_PANEL: import.meta.env.VITE_FLAG_WORKSPACE_FLOW_PANEL === 'true',
    /**
     * Show the search and filter panel for the Protocol Adapter
     */
    PROTOCOL_ADAPTER_FACET: import.meta.env.VITE_FLAG_FACET_SEARCH === 'true',
    /**
     * Show the metrics panel on the dashboard (conditional to first use flag)
     */
    METRICS_SELECT_PANEL: true,
    /**
     * The initial list of metrics
     */
    METRICS_DEFAULTS: [
      'com.hivemq.edge.messages.incoming.connect.count',
      'com.hivemq.edge.subscriptions.overall.current',
    ],
  },

  documentation: {
    // TODO[NVL] Is this the right place?
    namespace: 'https://www.hivemq.com/solutions/manufacturing/smart-manufacturing-using-isa95-mqtt-sparkplug-and-uns/',
  },
}

export default config
