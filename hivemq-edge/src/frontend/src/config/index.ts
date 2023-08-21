const config = {
  environment: import.meta.env.MODE,

  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,

  version: import.meta.env.VITE_APP_VERSION,
  documentationUrl: import.meta.env.VITE_APP_DOCUMENTATION,

  httpClient: {
    axiosTimeout: 15000,
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
