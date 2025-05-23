import { SentryCliCommitsOptions, SentryCliNewDeployOptions } from "@sentry/cli";
import { Scope } from "@sentry/core";
import { UnpluginOptions } from "unplugin";
import { Logger } from "../sentry/logger";
import { IncludeEntry } from "../types";
import { Client } from "@sentry/types";
interface ReleaseManagementPluginOptions {
    logger: Logger;
    releaseName: string;
    shouldCreateRelease: boolean;
    shouldFinalizeRelease: boolean;
    include?: string | IncludeEntry | Array<string | IncludeEntry>;
    setCommitsOption?: SentryCliCommitsOptions;
    deployOptions?: SentryCliNewDeployOptions;
    dist?: string;
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
export declare function releaseManagementPlugin({ releaseName, include, dist, setCommitsOption, shouldCreateRelease, shouldFinalizeRelease, deployOptions, handleRecoverableError, sentryScope, sentryClient, sentryCliOptions, createDependencyOnSourcemapFiles, }: ReleaseManagementPluginOptions): UnpluginOptions;
export {};
//# sourceMappingURL=release-management.d.ts.map