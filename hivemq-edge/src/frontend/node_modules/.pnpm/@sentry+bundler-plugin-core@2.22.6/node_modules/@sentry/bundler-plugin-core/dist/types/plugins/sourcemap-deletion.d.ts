import { UnpluginOptions } from "unplugin";
import { Logger } from "../sentry/logger";
import { Scope } from "@sentry/core";
import { Client } from "@sentry/types";
interface FileDeletionPlugin {
    handleRecoverableError: (error: unknown) => void;
    waitUntilSourcemapFileDependenciesAreFreed: () => Promise<void>;
    sentryScope: Scope;
    sentryClient: Client;
    filesToDeleteAfterUpload: string | string[] | undefined;
    logger: Logger;
}
export declare function fileDeletionPlugin({ handleRecoverableError, sentryScope, sentryClient, filesToDeleteAfterUpload, waitUntilSourcemapFileDependenciesAreFreed, logger, }: FileDeletionPlugin): UnpluginOptions;
export {};
//# sourceMappingURL=sourcemap-deletion.d.ts.map