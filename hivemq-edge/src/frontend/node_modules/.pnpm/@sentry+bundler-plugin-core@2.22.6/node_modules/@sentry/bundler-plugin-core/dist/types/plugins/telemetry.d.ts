import { Scope } from "@sentry/core";
import { Client } from "@sentry/types";
import { UnpluginOptions } from "unplugin";
import { Logger } from "../sentry/logger";
interface TelemetryPluginOptions {
    sentryClient: Client;
    sentryScope: Scope;
    shouldSendTelemetry: Promise<boolean>;
    logger: Logger;
}
export declare function telemetryPlugin({ sentryClient, sentryScope, shouldSendTelemetry, logger, }: TelemetryPluginOptions): UnpluginOptions;
export {};
//# sourceMappingURL=telemetry.d.ts.map