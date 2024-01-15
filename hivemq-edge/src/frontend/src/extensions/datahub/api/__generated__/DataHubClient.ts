/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { AxiosHttpRequest } from './core/AxiosHttpRequest';

import { BackupRestoreService } from './services/BackupRestoreService';
import { DataHubBehaviorPoliciesService } from './services/DataHubBehaviorPoliciesService';
import { DataHubDataPoliciesService } from './services/DataHubDataPoliciesService';
import { DataHubManagementService } from './services/DataHubManagementService';
import { DataHubSchemasService } from './services/DataHubSchemasService';
import { DataHubScriptsService } from './services/DataHubScriptsService';
import { DataHubStateService } from './services/DataHubStateService';
import { DiagnosticArchiveService } from './services/DiagnosticArchiveService';
import { MqttClientsService } from './services/MqttClientsService';
import { TraceRecordingsService } from './services/TraceRecordingsService';

type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;

export class DataHubClient {

    public readonly backupRestore: BackupRestoreService;
    public readonly dataHubBehaviorPolicies: DataHubBehaviorPoliciesService;
    public readonly dataHubDataPolicies: DataHubDataPoliciesService;
    public readonly dataHubManagement: DataHubManagementService;
    public readonly dataHubSchemas: DataHubSchemasService;
    public readonly dataHubScripts: DataHubScriptsService;
    public readonly dataHubState: DataHubStateService;
    public readonly diagnosticArchive: DiagnosticArchiveService;
    public readonly mqttClients: MqttClientsService;
    public readonly traceRecordings: TraceRecordingsService;

    public readonly request: BaseHttpRequest;

    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = AxiosHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? '',
            VERSION: config?.VERSION ?? '4.24.0',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });

        this.backupRestore = new BackupRestoreService(this.request);
        this.dataHubBehaviorPolicies = new DataHubBehaviorPoliciesService(this.request);
        this.dataHubDataPolicies = new DataHubDataPoliciesService(this.request);
        this.dataHubManagement = new DataHubManagementService(this.request);
        this.dataHubSchemas = new DataHubSchemasService(this.request);
        this.dataHubScripts = new DataHubScriptsService(this.request);
        this.dataHubState = new DataHubStateService(this.request);
        this.diagnosticArchive = new DiagnosticArchiveService(this.request);
        this.mqttClients = new MqttClientsService(this.request);
        this.traceRecordings = new TraceRecordingsService(this.request);
    }
}

