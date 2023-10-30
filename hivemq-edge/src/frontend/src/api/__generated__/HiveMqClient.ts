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
import { DefaultService } from './services/DefaultService';
import { EventsService } from './services/EventsService';
import { FrontendService } from './services/FrontendService';
import { GatewayEndpointService } from './services/GatewayEndpointService';
import { MetricsService } from './services/MetricsService';
import { MetricsEndpointService } from './services/MetricsEndpointService';
import { ProtocolAdaptersService } from './services/ProtocolAdaptersService';
import { UnsService } from './services/UnsService';

type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;

export class HiveMqClient {

    public readonly authentication: AuthenticationService;
    public readonly authenticationEndpoint: AuthenticationEndpointService;
    public readonly bridges: BridgesService;
    public readonly default: DefaultService;
    public readonly events: EventsService;
    public readonly frontend: FrontendService;
    public readonly gatewayEndpoint: GatewayEndpointService;
    public readonly metrics: MetricsService;
    public readonly metricsEndpoint: MetricsEndpointService;
    public readonly protocolAdapters: ProtocolAdaptersService;
    public readonly uns: UnsService;

    public readonly request: BaseHttpRequest;

    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = AxiosHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? '',
            VERSION: config?.VERSION ?? '2023.8',
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
        this.default = new DefaultService(this.request);
        this.events = new EventsService(this.request);
        this.frontend = new FrontendService(this.request);
        this.gatewayEndpoint = new GatewayEndpointService(this.request);
        this.metrics = new MetricsService(this.request);
        this.metricsEndpoint = new MetricsEndpointService(this.request);
        this.protocolAdapters = new ProtocolAdaptersService(this.request);
        this.uns = new UnsService(this.request);
    }
}

