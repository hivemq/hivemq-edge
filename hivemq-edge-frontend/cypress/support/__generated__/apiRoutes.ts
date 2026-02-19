/**
 * Type-safe Cypress intercept route registry.
 *
 * GENERATED FILE — do not edit manually.
 * Regenerate with: pnpm dev:openAPI
 *
 * @see {@link https://linear.app/hivemq/issue/EDG-73} for implementation task
 *
 * ## Usage
 *
 * ```typescript
 * // ✅ TypeScript validates the response shape against the OpenAPI model
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake_token' })
 *
 * // ✅ Status-only shorthand works for any route
 * cy.interceptApi(API_ROUTES.bridges.getBridges, { statusCode: 404 })
 *
 * // ✅ Parametric route with ** wildcard (matches any ID)
 * cy.interceptApi(API_ROUTES.bridges.getBridgeByName, { ...bridgeMock })
 *
 * // ✅ Parametric route with specific ID
 * cy.interceptApi(API_ROUTES.bridges.getBridgeByName.withParams({ bridgeId: 'my-bridge' }), { ...bridgeMock })
 *
 * // ❌ TypeScript error — wrong shape for the route
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { items: [] })
 * ```
 */

import type {
  Adapter,
  AdaptersList,
  ApiBearerToken,
  BehaviorPolicy,
  BehaviorPolicyList,
  Bridge,
  BridgeList,
  CapabilityList,
  Combiner,
  CombinerList,
  DataCombiningList,
  DataPoint,
  DataPolicy,
  DataPolicyList,
  DomainTag,
  DomainTagList,
  DomainTagOwnerList,
  EventList,
  FsmStatesInformationListItem,
  FunctionSpecsList,
  GatewayConfiguration,
  HealthStatus,
  ISA95ApiBean,
  Instruction,
  InterpolationVariableList,
  JsonNode,
  ListenerList,
  ManagedAssetList,
  MetricList,
  NorthboundMappingList,
  NorthboundMappingOwnerList,
  NotificationList,
  PayloadSampleList,
  PolicySchema,
  ProtocolAdaptersList,
  PulseStatus,
  SchemaList,
  Script,
  ScriptList,
  SouthboundMappingList,
  SouthboundMappingOwnerList,
  Status,
  StatusList,
  StatusTransitionResult,
  TagSchema,
  TopicFilter,
  TopicFilterList,
  ValuesTree,
} from '@/api/__generated__'

// ─── Core types ───────────────────────────────────────────────────────────────

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

/**
 * A route descriptor carrying the response type as a phantom type parameter.
 * `T` is never stored at runtime — it only exists for TypeScript inference.
 */
export type Route<T> = {
  readonly method: HttpMethod
  readonly url: string
  /** Phantom type — undefined at runtime, inferred by TypeScript. Do not access. */
  readonly _responseType: T
}

type ExtractUrlParams<Template extends string> = Template extends `${string}{${infer Param}}${infer Rest}`
  ? Param | ExtractUrlParams<Rest>
  : never

type UrlParams<Template extends string> = { [K in ExtractUrlParams<Template>]: string }

/**
 * A ParametricRoute extends Route<T> (usable as-is with ** glob wildcard) and
 * adds withParams() to resolve specific path parameter values into an exact URL.
 *
 * TypeScript infers the URL template as a literal type and enforces the correct
 * parameter names in withParams().
 */
export type ParametricRoute<T, Template extends string> = Route<T> & {
  readonly urlTemplate: Template
  /**
   * Resolve the URL template with specific path parameter values.
   * Returns a plain Route<T> with an exact URL (no wildcards).
   *
   * @example
   * API_ROUTES.bridges.getBridgeByName.withParams({ bridgeId: 'my-bridge' })
   * // → Route<Bridge> with url '/api/v1/management/bridges/my-bridge'
   */
  withParams(params: UrlParams<Template>): Route<T>
}

// ─── Factories ────────────────────────────────────────────────────────────────

/** Factory for routes with no path parameters. */
export const route = <T>(method: HttpMethod, url: string): Route<T> => ({
  method,
  url,
  _responseType: undefined as unknown as T,
})

/**
 * Curried factory for routes with path parameters.
 *
 * TypeScript cannot partially infer type arguments in a single generic call, so
 * this factory uses currying: specify T in the outer call, let TypeScript infer
 * Template from the URL string literal in the inner call.
 *
 * The default url replaces each {param} placeholder with ** (matches any value).
 *
 * @example
 * routeWithParams<Bridge>()('GET', '/api/v1/management/bridges/{bridgeId}')
 * // T = Bridge, Template = '/api/v1/management/bridges/{bridgeId}'
 * // .withParams({ bridgeId }) is enforced; .withParams({ id }) is a TS error
 */
export const routeWithParams =
  <T>() =>
  <Template extends string>(method: HttpMethod, urlTemplate: Template): ParametricRoute<T, Template> => {
    const defaultUrl = (urlTemplate as string).replace(/\{[^}]+}/g, '**')
    return {
      method,
      url: defaultUrl,
      urlTemplate,
      _responseType: undefined as unknown as T,
      withParams(params: UrlParams<Template>): Route<T> {
        const resolvedUrl = (urlTemplate as string).replace(
          /\{([^}]+)}/g,
          (_, name) => (params as Record<string, string>)[name] ?? '**'
        )
        return { method, url: resolvedUrl, _responseType: undefined as unknown as T }
      },
    }
  }

// ─── Registry ─────────────────────────────────────────────────────────────────

/**
 * Registry of all typed API routes, namespaced to mirror the HiveMqClient service structure.
 *
 * The namespace matches how the app accesses services in production:
 * - `appClient.authentication.authenticate()` → `API_ROUTES.authenticationEndpoint.authenticate`
 * - `appClient.bridges.getBridges()`           → `API_ROUTES.bridges.getBridges`
 *
 * Go-to-definition on any route opens its registry entry with a @see JSDoc link to the
 * service class and method that owns it.
 */
export const API_ROUTES = {
  /**
   * @see {@link AuthenticationService}
   */
  authentication: {
    /**
     * @see {@link AuthenticationService.authenticate}
     */
    authenticate: route<ApiBearerToken>('POST', '/api/v1/auth/authenticate'),

    /**
     * @see {@link AuthenticationService.refreshToken}
     */
    refreshToken: route<ApiBearerToken>('POST', '/api/v1/auth/refresh-token'),

    /**
     * @see {@link AuthenticationService.validateToken}
     */
    validateToken: route<void>('POST', '/api/v1/auth/validate-token'),
  },

  /**
   * @see {@link AuthenticationEndpointService}
   */
  authenticationEndpoint: {
    /**
     * @see {@link AuthenticationEndpointService.authenticate}
     */
    authenticate: route<ApiBearerToken>('POST', '/api/v1/auth/authenticate'),

    /**
     * @see {@link AuthenticationEndpointService.refreshToken}
     */
    refreshToken: route<ApiBearerToken>('POST', '/api/v1/auth/refresh-token'),

    /**
     * @see {@link AuthenticationEndpointService.validateToken}
     */
    validateToken: route<void>('POST', '/api/v1/auth/validate-token'),
  },

  /**
   * @see {@link BridgesService}
   */
  bridges: {
    /**
     * @see {@link BridgesService.getBridges}
     */
    getBridges: route<BridgeList>('GET', '/api/v1/management/bridges'),

    /**
     * @see {@link BridgesService.addBridge}
     */
    addBridge: route<void>('POST', '/api/v1/management/bridges'),

    /**
     * @see {@link BridgesService.getBridgesStatus}
     */
    getBridgesStatus: route<StatusList>('GET', '/api/v1/management/bridges/status'),

    /**
     * @see {@link BridgesService.removeBridge}
     */
    removeBridge: routeWithParams<void>()('DELETE', '/api/v1/management/bridges/{bridgeId}'),

    /**
     * @see {@link BridgesService.getBridgeByName}
     */
    getBridgeByName: routeWithParams<Bridge>()('GET', '/api/v1/management/bridges/{bridgeId}'),

    /**
     * @see {@link BridgesService.updateBridge}
     */
    updateBridge: routeWithParams<void>()('PUT', '/api/v1/management/bridges/{bridgeId}'),

    /**
     * @see {@link BridgesService.getBridgeStatus}
     */
    getBridgeStatus: routeWithParams<Status>()('GET', '/api/v1/management/bridges/{bridgeId}/connection-status'),

    /**
     * @see {@link BridgesService.transitionBridgeStatus}
     */
    transitionBridgeStatus: routeWithParams<StatusTransitionResult>()('PUT', '/api/v1/management/bridges/{bridgeId}/status'),
  },

  /**
   * @see {@link CombinersService}
   */
  combiners: {
    /**
     * @see {@link CombinersService.getCombiners}
     */
    getCombiners: route<CombinerList>('GET', '/api/v1/management/combiners'),

    /**
     * @see {@link CombinersService.addCombiner}
     */
    addCombiner: route<void>('POST', '/api/v1/management/combiners'),

    /**
     * @see {@link CombinersService.getCombinersById}
     */
    getCombinersById: routeWithParams<Combiner>()('GET', '/api/v1/management/combiners/{combinerId}'),

    /**
     * @see {@link CombinersService.deleteCombiner}
     */
    deleteCombiner: routeWithParams<void>()('DELETE', '/api/v1/management/combiners/{combinerId}'),

    /**
     * @see {@link CombinersService.updateCombiner}
     */
    updateCombiner: routeWithParams<void>()('PUT', '/api/v1/management/combiners/{combinerId}'),

    /**
     * @see {@link CombinersService.getCombinerMappings}
     */
    getCombinerMappings: routeWithParams<DataCombiningList>()('GET', '/api/v1/management/combiners/{combinerId}/mappings'),

    /**
     * @see {@link CombinersService.getMappingInstructions}
     */
    getMappingInstructions: routeWithParams<Array<Instruction>>()('GET', '/api/v1/management/combiners/{combinerId}/mappings/{mappingId}/instructions'),
  },

  /**
   * @see {@link DataHubBehaviorPoliciesService}
   */
  dataHubBehaviorPolicies: {
    /**
     * @see {@link DataHubBehaviorPoliciesService.getAllBehaviorPolicies}
     */
    getAllBehaviorPolicies: route<BehaviorPolicyList>('GET', '/api/v1/data-hub/behavior-validation/policies'),

    /**
     * @see {@link DataHubBehaviorPoliciesService.createBehaviorPolicy}
     */
    createBehaviorPolicy: route<BehaviorPolicy>('POST', '/api/v1/data-hub/behavior-validation/policies'),

    /**
     * @see {@link DataHubBehaviorPoliciesService.deleteBehaviorPolicy}
     */
    deleteBehaviorPolicy: routeWithParams<void>()('DELETE', '/api/v1/data-hub/behavior-validation/policies/{policyId}'),

    /**
     * @see {@link DataHubBehaviorPoliciesService.getBehaviorPolicy}
     */
    getBehaviorPolicy: routeWithParams<BehaviorPolicy>()('GET', '/api/v1/data-hub/behavior-validation/policies/{policyId}'),

    /**
     * @see {@link DataHubBehaviorPoliciesService.updateBehaviorPolicy}
     */
    updateBehaviorPolicy: routeWithParams<BehaviorPolicy>()('PUT', '/api/v1/data-hub/behavior-validation/policies/{policyId}'),
  },

  /**
   * @see {@link DataHubDataPoliciesService}
   */
  dataHubDataPolicies: {
    /**
     * @see {@link DataHubDataPoliciesService.getAllDataPolicies}
     */
    getAllDataPolicies: route<DataPolicyList>('GET', '/api/v1/data-hub/data-validation/policies'),

    /**
     * @see {@link DataHubDataPoliciesService.createDataPolicy}
     */
    createDataPolicy: route<DataPolicy>('POST', '/api/v1/data-hub/data-validation/policies'),

    /**
     * @see {@link DataHubDataPoliciesService.deleteDataPolicy}
     */
    deleteDataPolicy: routeWithParams<void>()('DELETE', '/api/v1/data-hub/data-validation/policies/{policyId}'),

    /**
     * @see {@link DataHubDataPoliciesService.getDataPolicy}
     */
    getDataPolicy: routeWithParams<DataPolicy>()('GET', '/api/v1/data-hub/data-validation/policies/{policyId}'),

    /**
     * @see {@link DataHubDataPoliciesService.updateDataPolicy}
     */
    updateDataPolicy: routeWithParams<DataPolicy>()('PUT', '/api/v1/data-hub/data-validation/policies/{policyId}'),
  },

  /**
   * @see {@link DataHubFsmService}
   */
  dataHubFsm: {
    /**
     * @see {@link DataHubFsmService.getFsms}
     */
    getFsms: route<JsonNode>('GET', '/api/v1/data-hub/fsm'),
  },

  /**
   * @see {@link DataHubFunctionsService}
   */
  dataHubFunctions: {
    /**
     * @see {@link DataHubFunctionsService.getFunctions}
     */
    getFunctions: route<JsonNode>('GET', '/api/v1/data-hub/functions'),

    /**
     * @see {@link DataHubFunctionsService.getFunctionSpecs}
     */
    getFunctionSpecs: route<FunctionSpecsList>('GET', '/api/v1/data-hub/function-specs'),
  },

  /**
   * @see {@link DataHubInterpolationService}
   */
  dataHubInterpolation: {
    /**
     * @see {@link DataHubInterpolationService.getVariables}
     */
    getVariables: route<InterpolationVariableList>('GET', '/api/v1/data-hub/interpolation-variables'),
  },

  /**
   * @see {@link DataHubSchemasService}
   */
  dataHubSchemas: {
    /**
     * @see {@link DataHubSchemasService.getAllSchemas}
     */
    getAllSchemas: route<SchemaList>('GET', '/api/v1/data-hub/schemas'),

    /**
     * @see {@link DataHubSchemasService.createSchema}
     */
    createSchema: route<PolicySchema>('POST', '/api/v1/data-hub/schemas'),

    /**
     * @see {@link DataHubSchemasService.deleteSchema}
     */
    deleteSchema: routeWithParams<void>()('DELETE', '/api/v1/data-hub/schemas/{schemaId}'),

    /**
     * @see {@link DataHubSchemasService.getSchema}
     */
    getSchema: routeWithParams<PolicySchema>()('GET', '/api/v1/data-hub/schemas/{schemaId}'),
  },

  /**
   * @see {@link DataHubScriptsService}
   */
  dataHubScripts: {
    /**
     * @see {@link DataHubScriptsService.getAllScripts}
     */
    getAllScripts: route<ScriptList>('GET', '/api/v1/data-hub/scripts'),

    /**
     * @see {@link DataHubScriptsService.createScript}
     */
    createScript: route<Script>('POST', '/api/v1/data-hub/scripts'),

    /**
     * @see {@link DataHubScriptsService.deleteScript}
     */
    deleteScript: routeWithParams<void>()('DELETE', '/api/v1/data-hub/scripts/{scriptId}'),

    /**
     * @see {@link DataHubScriptsService.getScript}
     */
    getScript: routeWithParams<Script>()('GET', '/api/v1/data-hub/scripts/{scriptId}'),
  },

  /**
   * @see {@link DataHubStateService}
   */
  dataHubState: {
    /**
     * @see {@link DataHubStateService.getClientState}
     */
    getClientState: routeWithParams<FsmStatesInformationListItem>()('GET', '/api/v1/data-hub/behavior-validation/states/{clientId}'),
  },

  /**
   * @see {@link DomainService}
   */
  domain: {
    /**
     * @see {@link DomainService.getNorthboundMappings}
     */
    getNorthboundMappings: route<NorthboundMappingOwnerList>('GET', '/api/v1/management/protocol-adapters/mappings/northboundMappings'),

    /**
     * @see {@link DomainService.getSouthboundMappings}
     */
    getSouthboundMappings: route<SouthboundMappingOwnerList>('GET', '/api/v1/management/protocol-adapters/mappings/southboundMappings'),

    /**
     * @see {@link DomainService.getDomainTags}
     */
    getDomainTags: route<DomainTagOwnerList>('GET', '/api/v1/management/protocol-adapters/tags'),

    /**
     * @see {@link DomainService.getTopicFilters}
     */
    getTopicFilters: route<TopicFilterList>('GET', '/api/v1/management/topic-filters'),

    /**
     * @see {@link DomainService.getManagedAssets}
     */
    getManagedAssets: route<ManagedAssetList>('GET', '/api/v1/management/pulse/managed-assets'),
  },

  /**
   * @see {@link EventsService}
   */
  events: {
    /**
     * @see {@link EventsService.getEvents}
     */
    getEvents: route<EventList>('GET', '/api/v1/management/events'),
  },

  /**
   * @see {@link FrontendService}
   */
  frontend: {
    /**
     * @see {@link FrontendService.getCapabilities}
     */
    getCapabilities: route<CapabilityList>('GET', '/api/v1/frontend/capabilities'),

    /**
     * @see {@link FrontendService.getConfiguration}
     */
    getConfiguration: route<GatewayConfiguration>('GET', '/api/v1/frontend/configuration'),

    /**
     * @see {@link FrontendService.getNotifications}
     */
    getNotifications: route<NotificationList>('GET', '/api/v1/frontend/notifications'),
  },

  /**
   * @see {@link GatewayEndpointService}
   */
  gatewayEndpoint: {
    /**
     * @see {@link GatewayEndpointService.getXmlConfiguration}
     */
    getXmlConfiguration: route<string>('GET', '/api/v1/gateway/configuration'),

    /**
     * @see {@link GatewayEndpointService.getListeners}
     */
    getListeners: route<ListenerList>('GET', '/api/v1/gateway/listeners'),
  },

  /**
   * @see {@link HealthCheckEndpointService}
   */
  healthCheckEndpoint: {
    /**
     * @see {@link HealthCheckEndpointService.liveness}
     */
    liveness: route<HealthStatus>('GET', '/api/v1/health/liveness'),

    /**
     * @see {@link HealthCheckEndpointService.readiness}
     */
    readiness: route<HealthStatus>('GET', '/api/v1/health/readiness'),
  },

  /**
   * @see {@link MetricsService}
   */
  metrics: {
    /**
     * @see {@link MetricsService.getMetrics}
     */
    getMetrics: route<MetricList>('GET', '/api/v1/metrics'),

    /**
     * @see {@link MetricsService.getSample}
     */
    getSample: routeWithParams<DataPoint>()('GET', '/api/v1/metrics/{metricName}/latest'),
  },

  /**
   * @see {@link MetricsEndpointService}
   */
  metricsEndpoint: {
    /**
     * @see {@link MetricsEndpointService.getMetrics}
     */
    getMetrics: route<MetricList>('GET', '/api/v1/metrics'),

    /**
     * @see {@link MetricsEndpointService.getSample}
     */
    getSample: routeWithParams<DataPoint>()('GET', '/api/v1/metrics/{metricName}/latest'),
  },

  /**
   * @see {@link PayloadSamplingService}
   */
  payloadSampling: {
    /**
     * @see {@link PayloadSamplingService.getSchemaForTopic}
     */
    getSchemaForTopic: routeWithParams<JsonNode>()('GET', '/api/v1/management/sampling/schema/{topic}'),

    /**
     * @see {@link PayloadSamplingService.getSamplesForTopic}
     */
    getSamplesForTopic: routeWithParams<PayloadSampleList>()('GET', '/api/v1/management/sampling/topic/{topic}'),

    /**
     * @see {@link PayloadSamplingService.startSamplingForTopic}
     */
    startSamplingForTopic: routeWithParams<void>()('POST', '/api/v1/management/sampling/topic/{topic}'),
  },

  /**
   * @see {@link ProtocolAdaptersService}
   */
  protocolAdapters: {
    /**
     * @see {@link ProtocolAdaptersService.createCompleteAdapter}
     */
    createCompleteAdapter: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapterconfigs/{adaptertype}/{adaptername}'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapters}
     */
    getAdapters: route<AdaptersList>('GET', '/api/v1/management/protocol-adapters/adapters'),

    /**
     * @see {@link ProtocolAdaptersService.deleteAdapter}
     */
    deleteAdapter: routeWithParams<void>()('DELETE', '/api/v1/management/protocol-adapters/adapters/{adapterId}'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapter}
     */
    getAdapter: routeWithParams<Adapter>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}'),

    /**
     * @see {@link ProtocolAdaptersService.updateAdapter}
     */
    updateAdapter: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}'),

    /**
     * @see {@link ProtocolAdaptersService.discoverDataPoints}
     */
    discoverDataPoints: routeWithParams<ValuesTree>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}/discover'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapterNorthboundMappings}
     */
    getAdapterNorthboundMappings: routeWithParams<NorthboundMappingList>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}/northboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.updateAdapterNorthboundMappings}
     */
    updateAdapterNorthboundMappings: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}/northboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapterSouthboundMappings}
     */
    getAdapterSouthboundMappings: routeWithParams<SouthboundMappingList>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}/southboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.updateAdapterSouthboundMappings}
     */
    updateAdapterSouthboundMappings: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}/southboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapterStatus}
     */
    getAdapterStatus: routeWithParams<Status>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}/status'),

    /**
     * @see {@link ProtocolAdaptersService.transitionAdapterStatus}
     */
    transitionAdapterStatus: routeWithParams<StatusTransitionResult>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}/status'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapterDomainTags}
     */
    getAdapterDomainTags: routeWithParams<DomainTagList>()('GET', '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags'),

    /**
     * @see {@link ProtocolAdaptersService.addAdapterDomainTags}
     */
    addAdapterDomainTags: routeWithParams<void>()('POST', '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags'),

    /**
     * @see {@link ProtocolAdaptersService.updateAdapterDomainTags}
     */
    updateAdapterDomainTags: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags'),

    /**
     * @see {@link ProtocolAdaptersService.deleteAdapterDomainTags}
     */
    deleteAdapterDomainTags: routeWithParams<void>()('DELETE', '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagName}'),

    /**
     * @see {@link ProtocolAdaptersService.updateAdapterDomainTag}
     */
    updateAdapterDomainTag: routeWithParams<void>()('PUT', '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagName}'),

    /**
     * @see {@link ProtocolAdaptersService.addAdapter}
     */
    addAdapter: routeWithParams<void>()('POST', '/api/v1/management/protocol-adapters/adapters/{adapterType}'),

    /**
     * @see {@link ProtocolAdaptersService.getNorthboundMappings}
     */
    getNorthboundMappings: route<NorthboundMappingOwnerList>('GET', '/api/v1/management/protocol-adapters/mappings/northboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.getSouthboundMappings}
     */
    getSouthboundMappings: route<SouthboundMappingOwnerList>('GET', '/api/v1/management/protocol-adapters/mappings/southboundMappings'),

    /**
     * @see {@link ProtocolAdaptersService.getAdaptersStatus}
     */
    getAdaptersStatus: route<StatusList>('GET', '/api/v1/management/protocol-adapters/status'),

    /**
     * @see {@link ProtocolAdaptersService.getTagSchema}
     */
    getTagSchema: routeWithParams<TagSchema>()('GET', '/api/v1/management/protocol-adapters/tag-schemas/{protocolId}'),

    /**
     * @see {@link ProtocolAdaptersService.getDomainTags}
     */
    getDomainTags: route<DomainTagOwnerList>('GET', '/api/v1/management/protocol-adapters/tags'),

    /**
     * @see {@link ProtocolAdaptersService.getDomainTag}
     */
    getDomainTag: routeWithParams<DomainTag>()('GET', '/api/v1/management/protocol-adapters/tags/{tagName}'),

    /**
     * @see {@link ProtocolAdaptersService.getAdapterTypes}
     */
    getAdapterTypes: route<ProtocolAdaptersList>('GET', '/api/v1/management/protocol-adapters/types'),

    /**
     * @see {@link ProtocolAdaptersService.getAdaptersForType}
     */
    getAdaptersForType: routeWithParams<AdaptersList>()('GET', '/api/v1/management/protocol-adapters/types/{adapterType}'),

    /**
     * @see {@link ProtocolAdaptersService.getWritingSchema}
     */
    getWritingSchema: routeWithParams<JsonNode>()('GET', '/api/v1/management/protocol-adapters/writing-schema/{adapterId}/{tagName}'),
  },

  /**
   * @see {@link PulseService}
   */
  pulse: {
    /**
     * @see {@link PulseService.deletePulseActivationToken}
     */
    deletePulseActivationToken: route<void>('DELETE', '/api/v1/management/pulse/activation-token'),

    /**
     * @see {@link PulseService.updatePulseActivationToken}
     */
    updatePulseActivationToken: route<void>('POST', '/api/v1/management/pulse/activation-token'),

    /**
     * @see {@link PulseService.getPulseStatus}
     */
    getPulseStatus: route<PulseStatus>('GET', '/api/v1/management/pulse/status'),

    /**
     * @see {@link PulseService.getManagedAssets}
     */
    getManagedAssets: route<ManagedAssetList>('GET', '/api/v1/management/pulse/managed-assets'),

    /**
     * @see {@link PulseService.addManagedAsset}
     */
    addManagedAsset: route<void>('POST', '/api/v1/management/pulse/managed-assets'),

    /**
     * @see {@link PulseService.deleteManagedAsset}
     */
    deleteManagedAsset: routeWithParams<void>()('DELETE', '/api/v1/management/pulse/managed-assets/{assetId}'),

    /**
     * @see {@link PulseService.updateManagedAsset}
     */
    updateManagedAsset: routeWithParams<void>()('PUT', '/api/v1/management/pulse/managed-assets/{assetId}'),

    /**
     * @see {@link PulseService.getAssetMappers}
     */
    getAssetMappers: route<CombinerList>('GET', '/api/v1/management/pulse/asset-mappers'),

    /**
     * @see {@link PulseService.addAssetMapper}
     */
    addAssetMapper: route<void>('POST', '/api/v1/management/pulse/asset-mappers'),

    /**
     * @see {@link PulseService.getAssetMapper}
     */
    getAssetMapper: routeWithParams<Combiner>()('GET', '/api/v1/management/pulse/asset-mappers/{combinerId}'),

    /**
     * @see {@link PulseService.deleteAssetMapper}
     */
    deleteAssetMapper: routeWithParams<void>()('DELETE', '/api/v1/management/pulse/asset-mappers/{combinerId}'),

    /**
     * @see {@link PulseService.updateAssetMapper}
     */
    updateAssetMapper: routeWithParams<void>()('PUT', '/api/v1/management/pulse/asset-mappers/{combinerId}'),

    /**
     * @see {@link PulseService.getAssetMapperMappings}
     */
    getAssetMapperMappings: routeWithParams<DataCombiningList>()('GET', '/api/v1/management/pulse/asset-mappers/{combinerId}/mappings'),

    /**
     * @see {@link PulseService.getAssetMapperInstructions}
     */
    getAssetMapperInstructions: routeWithParams<Array<Instruction>>()('GET', '/api/v1/management/pulse/asset-mappers/{combinerId}/mappings/{mappingId}/instructions'),
  },

  /**
   * @see {@link TopicFiltersService}
   */
  topicFilters: {
    /**
     * @see {@link TopicFiltersService.getTopicFilters}
     */
    getTopicFilters: route<TopicFilterList>('GET', '/api/v1/management/topic-filters'),

    /**
     * @see {@link TopicFiltersService.addTopicFilters}
     */
    addTopicFilters: route<void>('POST', '/api/v1/management/topic-filters'),

    /**
     * @see {@link TopicFiltersService.updateTopicFilters}
     */
    updateTopicFilters: route<void>('PUT', '/api/v1/management/topic-filters'),

    /**
     * @see {@link TopicFiltersService.getTopicFilter}
     */
    getTopicFilter: routeWithParams<TopicFilter>()('GET', '/api/v1/management/topic-filters/{filter}'),

    /**
     * @see {@link TopicFiltersService.deleteTopicFilter}
     */
    deleteTopicFilter: routeWithParams<void>()('DELETE', '/api/v1/management/topic-filters/{filter}'),

    /**
     * @see {@link TopicFiltersService.updateTopicFilter}
     */
    updateTopicFilter: routeWithParams<void>()('PUT', '/api/v1/management/topic-filters/{filter}'),

    /**
     * @see {@link TopicFiltersService.getTopicFilterSchema}
     */
    getTopicFilterSchema: routeWithParams<string>()('GET', '/api/v1/management/topic-filters/{filter}/schema'),
  },

  /**
   * @see {@link UnsService}
   */
  uns: {
    /**
     * @see {@link UnsService.getIsa95}
     */
    getIsa95: route<ISA95ApiBean>('GET', '/api/v1/management/uns/isa95'),

    /**
     * @see {@link UnsService.setIsa95}
     */
    setIsa95: route<void>('POST', '/api/v1/management/uns/isa95'),
  },
} as const
