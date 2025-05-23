declare const themeInterfaceDestination: string[];
/**
 * Find the location of the default target file or resolve the given path
 */
declare function resolveOutputPath(overridePath?: string): Promise<string>;

export { resolveOutputPath, themeInterfaceDestination };
