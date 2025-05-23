import { Logger } from "./sentry/logger";
import { Options as UserOptions } from "./types";
export type NormalizedOptions = ReturnType<typeof normalizeUserOptions>;
export declare const SENTRY_SAAS_URL = "https://sentry.io";
export declare function normalizeUserOptions(userOptions: UserOptions): {
    org: string | undefined;
    project: string | undefined;
    authToken: string | undefined;
    url: string;
    headers: Record<string, string> | undefined;
    debug: boolean;
    silent: boolean;
    errorHandler: ((err: Error) => void) | undefined;
    telemetry: boolean;
    disable: boolean;
    sourcemaps: {
        disable?: boolean | undefined;
        assets?: string | string[] | undefined;
        ignore?: string | string[] | undefined;
        rewriteSources?: ((source: string, map: any) => string) | undefined;
        deleteFilesAfterUpload?: string | string[] | undefined;
        filesToDeleteAfterUpload?: string | string[] | undefined;
    } | undefined;
    release: {
        name: string | undefined;
        inject: boolean;
        create: boolean;
        finalize: boolean;
        vcsRemote: string;
        dist?: string | undefined;
        setCommits?: (({
            auto: true;
            repo?: undefined;
            commit?: undefined;
        } | {
            auto?: false | undefined;
            repo: string;
            commit: string;
        }) & {
            previousCommit?: string | undefined;
            ignoreMissing?: boolean | undefined;
            ignoreEmpty?: boolean | undefined;
        }) | undefined;
        deploy?: {
            env: string;
            started?: string | number | undefined;
            finished?: string | number | undefined;
            time?: number | undefined;
            name?: string | undefined;
            url?: string | undefined;
        } | undefined;
        cleanArtifacts?: boolean | undefined;
        uploadLegacySourcemaps?: string | import("./types").IncludeEntry | (string | import("./types").IncludeEntry)[] | undefined;
    };
    bundleSizeOptimizations: {
        excludeDebugStatements?: boolean | undefined;
        excludePerformanceMonitoring?: boolean | undefined;
        excludeTracing?: boolean | undefined;
        excludeReplayCanvas?: boolean | undefined;
        excludeReplayShadowDom?: boolean | undefined;
        excludeReplayIframe?: boolean | undefined;
        excludeReplayWorker?: boolean | undefined;
    } | undefined;
    reactComponentAnnotation: {
        enabled?: boolean | undefined;
    } | undefined;
    _metaOptions: {
        telemetry: {
            metaFramework: string | undefined;
        };
    };
    applicationKey: string | undefined;
    moduleMetadata: import("./types").ModuleMetadata | undefined;
    _experiments: {
        injectBuildInformation?: boolean | undefined;
        moduleMetadata?: import("./types").ModuleMetadata | import("./types").ModuleMetadataCallback | undefined;
    };
};
/**
 * Validates a few combinations of options that are not checked by Sentry CLI.
 *
 * For all other options, we can rely on Sentry CLI to validate them. In fact,
 * we can't validate them in the plugin because Sentry CLI might pick up options from
 * its config file.
 *
 * @param options the internal options
 * @param logger the logger
 *
 * @returns `true` if the options are valid, `false` otherwise
 */
export declare function validateOptions(options: NormalizedOptions, logger: Logger): boolean;
//# sourceMappingURL=options-mapping.d.ts.map