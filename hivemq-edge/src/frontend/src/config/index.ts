const config = {
  environment: import.meta.env.MODE,

  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,

  version: import.meta.env.VITE_APP_VERSION,
  documentationUrl: import.meta.env.VITE_APP_DOCUMENTATION,

  httpClient: {
    pollingRefetchInterval: 5 * 1000,
  },

  features: {
    /**
     * Show the search and filter panel for the Protocol Adapter
     */
    PROTOCOL_ADAPTER_FACET: false,
    /**
     * Show the metrics panel on the dashboard (conditional to first use flag)
     */
    METRICS_SELECT_PANEL: false,
    /**
     * The initial list of metrics
     */
    METRICS_DEFAULTS: ['com.hivemq.edge.messages.incoming.connect.count', 'com.hivemq.edge.subscriptions.overall.current'],
  },
}

export default config
