/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { AxiosHttpRequest } from './core/AxiosHttpRequest';

import { AuthenticationService } from './services/AuthenticationService';
import { AuthenticationEndpointService } from './services/AuthenticationEndpointService';
import { BridgesService } from './services/BridgesService';
import { ClientService } from './services/ClientService';
import { DataHubBehaviorPoliciesService } from './services/DataHubBehaviorPoliciesService';
import { DataHubDataPoliciesService } from './services/DataHubDataPoliciesService';
import { DataHubFsmService } from './services/DataHubFsmService';
import { DataHubFunctionsService } from './services/DataHubFunctionsService';
import { DataHubSchemasService } from './services/DataHubSchemasService';
import { DataHubScriptsService } from './services/DataHubScriptsService';
import { DataHubStateService } from './services/DataHubStateService';
import { DefaultService } from './services/DefaultService';
import { DomainService } from './services/DomainService';
import { EventsService } from './services/EventsService';
import { FrontendService } from './services/FrontendService';
import { GatewayEndpointService } from './services/GatewayEndpointService';
import { MetricsService } from './services/MetricsService';
import { MetricsEndpointService } from './services/MetricsEndpointService';
import { PayloadSamplingService } from './services/PayloadSamplingService';
import { ProtocolAdaptersService } from './services/ProtocolAdaptersService';
import { TopicFiltersService } from './services/TopicFiltersService';
import { UnsService } from './services/UnsService';

type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;

export class HiveMqClient {

    public readonly authentication: AuthenticationService;
    public readonly authenticationEndpoint: AuthenticationEndpointService;
    public readonly bridges: BridgesService;
    public readonly client: ClientService;
    public readonly dataHubBehaviorPolicies: DataHubBehaviorPoliciesService;
    public readonly dataHubDataPolicies: DataHubDataPoliciesService;
    public readonly dataHubFsm: DataHubFsmService;
    public readonly dataHubFunctions: DataHubFunctionsService;
    public readonly dataHubSchemas: DataHubSchemasService;
    public readonly dataHubScripts: DataHubScriptsService;
    public readonly dataHubState: DataHubStateService;
    public readonly default: DefaultService;
    public readonly domain: DomainService;
    public readonly events: EventsService;
    public readonly frontend: FrontendService;
    public readonly gatewayEndpoint: GatewayEndpointService;
    public readonly metrics: MetricsService;
    public readonly metricsEndpoint: MetricsEndpointService;
    public readonly payloadSampling: PayloadSamplingService;
    public readonly protocolAdapters: ProtocolAdaptersService;
    public readonly topicFilters: TopicFiltersService;
    public readonly uns: UnsService;

    public readonly request: BaseHttpRequest;

    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = AxiosHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? '',
            VERSION: config?.VERSION ?? '2024.8-SNAPSHOT',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });

        this.authentication = new AuthenticationService(this.request);
        this.authenticationEndpoint = new AuthenticationEndpointService(this.request);
        this.bridges = new BridgesService(this.request);
        this.client = new ClientService(this.request);
        this.dataHubBehaviorPolicies = new DataHubBehaviorPoliciesService(this.request);
        this.dataHubDataPolicies = new DataHubDataPoliciesService(this.request);
        this.dataHubFsm = new DataHubFsmService(this.request);
        this.dataHubFunctions = new DataHubFunctionsService(this.request);
        this.dataHubSchemas = new DataHubSchemasService(this.request);
        this.dataHubScripts = new DataHubScriptsService(this.request);
        this.dataHubState = new DataHubStateService(this.request);
        this.default = new DefaultService(this.request);
        this.domain = new DomainService(this.request);
        this.events = new EventsService(this.request);
        this.frontend = new FrontendService(this.request);
        this.gatewayEndpoint = new GatewayEndpointService(this.request);
        this.metrics = new MetricsService(this.request);
        this.metricsEndpoint = new MetricsEndpointService(this.request);
        this.payloadSampling = new PayloadSamplingService(this.request);
        this.protocolAdapters = new ProtocolAdaptersService(this.request);
        this.topicFilters = new TopicFiltersService(this.request);
        this.uns = new UnsService(this.request);
    }
}

