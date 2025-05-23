import { Logger } from "./sentry/logger";
import { Scope } from "@sentry/core";
import { Client } from "@sentry/types";
interface RewriteSourcesHook {
    (source: string, map: any): string;
}
interface DebugIdUploadPluginOptions {
    logger: Logger;
    assets?: string | string[];
    ignore?: string | string[];
    releaseName?: string;
    dist?: string;
    rewriteSourcesHook?: RewriteSourcesHook;
    handleRecoverableError: (error: unknown) => void;
    sentryScope: Scope;
    sentryClient: Client;
    sentryCliOptions: {
        url: string;
        authToken: string;
        org?: string;
        project: string;
        vcsRemote: string;
        silent: boolean;
        headers?: Record<string, string>;
    };
    createDependencyOnSourcemapFiles: () => () => void;
}
export declare function createDebugIdUploadFunction({ assets, ignore, logger, releaseName, dist, handleRecoverableError, sentryScope, sentryClient, sentryCliOptions, rewriteSourcesHook, createDependencyOnSourcemapFiles, }: DebugIdUploadPluginOptions): (buildArtifactPaths: string[]) => Promise<void>;
export declare function prepareBundleForDebugIdUpload(bundleFilePath: string, uploadFolder: string, chunkIndex: number, logger: Logger, rewriteSourcesHook: RewriteSourcesHook): Promise<void>;
export {};
//# sourceMappingURL=debug-id-upload.d.ts.map