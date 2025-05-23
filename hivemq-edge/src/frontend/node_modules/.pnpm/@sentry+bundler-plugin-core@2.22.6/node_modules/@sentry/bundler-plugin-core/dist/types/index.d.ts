import { TransformResult, UnpluginOptions } from "unplugin";
import { Logger } from "./sentry/logger";
import { Options, SentrySDKBuildFlags } from "./types";
interface SentryUnpluginFactoryOptions {
    releaseInjectionPlugin: (injectionCode: string) => UnpluginOptions;
    componentNameAnnotatePlugin?: () => UnpluginOptions;
    moduleMetadataInjectionPlugin: (injectionCode: string) => UnpluginOptions;
    debugIdInjectionPlugin: (logger: Logger) => UnpluginOptions;
    debugIdUploadPlugin: (upload: (buildArtifacts: string[]) => Promise<void>) => UnpluginOptions;
    bundleSizeOptimizationsPlugin: (buildFlags: SentrySDKBuildFlags) => UnpluginOptions;
}
/**
 * The sentry bundler plugin concerns itself with two things:
 * - Release injection
 * - Sourcemaps upload
 *
 * Release injection:
 * Per default the sentry bundler plugin will inject a global `SENTRY_RELEASE` into each JavaScript/TypeScript module
 * that is part of the bundle. On a technical level this is done by appending an import (`import "sentry-release-injector";`)
 * to all entrypoint files of the user code (see `transformInclude` and `transform` hooks). This import is then resolved
 * by the sentry plugin to a virtual module that sets the global variable (see `resolveId` and `load` hooks).
 * If a user wants to inject the release into a particular set of modules they can use the `releaseInjectionTargets` option.
 *
 * Source maps upload:
 *
 * The sentry bundler plugin will also take care of uploading source maps to Sentry. This
 * is all done in the `writeBundle` hook. In this hook the sentry plugin will execute the
 * release creation pipeline:
 *
 * 1. Create a new release
 * 2. Upload sourcemaps based on `include` and source-map-specific options
 * 3. Associate a range of commits with the release (if `setCommits` is specified)
 * 4. Finalize the release (unless `finalize` is disabled)
 * 5. Add deploy information to the release (if `deploy` is specified)
 *
 * This release creation pipeline relies on Sentry CLI to execute the different steps.
 */
export declare function sentryUnpluginFactory({ releaseInjectionPlugin, componentNameAnnotatePlugin, moduleMetadataInjectionPlugin, debugIdInjectionPlugin, debugIdUploadPlugin, bundleSizeOptimizationsPlugin, }: SentryUnpluginFactoryOptions): import("unplugin").UnpluginInstance<Options | undefined, true>;
export declare function getBuildInformation(): {
    deps: string[];
    depsVersions: Record<string, number>;
    nodeVersion: number | undefined;
};
/**
 * Determines whether the Sentry CLI binary is in its expected location.
 * This function is useful since `@sentry/cli` installs the binary via a post-install
 * script and post-install scripts may not always run. E.g. with `npm i --ignore-scripts`.
 */
export declare function sentryCliBinaryExists(): boolean;
export declare function createRollupReleaseInjectionHooks(injectionCode: string): {
    resolveId(id: string): {
        id: string;
        external: boolean;
        moduleSideEffects: boolean;
    } | null;
    load(id: string): string | null;
    transform(code: string, id: string): {
        code: string;
        map: import("magic-string").SourceMap;
    } | null;
};
export declare function createRollupBundleSizeOptimizationHooks(replacementValues: SentrySDKBuildFlags): {
    transform(code: string): {
        code: string;
        map: import("magic-string").SourceMap;
    } | null;
};
export declare function createRollupDebugIdInjectionHooks(): {
    renderChunk(code: string, chunk: {
        fileName: string;
    }): {
        code: string;
        map: import("magic-string").SourceMap;
    } | null;
};
export declare function createRollupModuleMetadataInjectionHooks(injectionCode: string): {
    renderChunk(code: string, chunk: {
        fileName: string;
    }): {
        code: string;
        map: import("magic-string").SourceMap;
    } | null;
};
export declare function createRollupDebugIdUploadHooks(upload: (buildArtifacts: string[]) => Promise<void>): {
    writeBundle(outputOptions: {
        dir?: string;
        file?: string;
    }, bundle: {
        [fileName: string]: unknown;
    }): Promise<void>;
};
export declare function createComponentNameAnnotateHooks(): {
    transform(this: void, code: string, id: string): Promise<TransformResult>;
};
export declare function getDebugIdSnippet(debugId: string): string;
export { stringToUUID, replaceBooleanFlagsInCode } from "./utils";
export type { Options, SentrySDKBuildFlags } from "./types";
export type { Logger } from "./sentry/logger";
//# sourceMappingURL=index.d.ts.map