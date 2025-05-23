interface ThemeKeyOptions {
    /**
     * Property key in the theme object
     * @example colors
     */
    key: string;
    /**
     * Maximum extraction level
     * @example
     * union: gray.500
     * level: 1---|2--|
     * @default 3
     */
    maxScanDepth?: number;
    /**
     * Pass a function to filter extracted values
     * @example
     * Exclude numeric index values from `breakpoints`
     * @default () => true
     */
    filter?: (value: string) => boolean;
    /**
     * Pass a function to flatMap extracted values
     * @default value => value
     */
    flatMap?: (value: string) => string | string[];
}
type TypingsTemplate = "default" | "augmentation";
interface CreateThemeTypingsInterfaceOptions {
    config: ThemeKeyOptions[];
    strictComponentTypes?: boolean;
    format?: boolean;
    strictTokenTypes?: boolean;
    template?: TypingsTemplate;
}
declare function createThemeTypingsInterface(theme: Record<string, unknown>, { config, strictComponentTypes, format, strictTokenTypes, template, }: CreateThemeTypingsInterfaceOptions): Promise<string>;

export { CreateThemeTypingsInterfaceOptions, ThemeKeyOptions, TypingsTemplate, createThemeTypingsInterface };
