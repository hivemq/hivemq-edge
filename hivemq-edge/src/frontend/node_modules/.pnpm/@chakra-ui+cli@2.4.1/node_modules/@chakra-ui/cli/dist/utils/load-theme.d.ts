declare function bundleConfigFile(fileName: string): Promise<{
    code: string;
    dependencies: string[];
}>;
declare function loadTheme(path: string): Promise<{
    theme: Record<string, any>;
    dependencies: string[];
}>;

export { bundleConfigFile, loadTheme };
