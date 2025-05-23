import { TypingsTemplate } from './create-theme-typings-interface.js';
export { themeInterfaceDestination } from './resolve-output-path.js';

declare function generateThemeTypings({ theme, out, strictComponentTypes, format, strictTokenTypes, template, onError, }: {
    theme: Record<string, any>;
    out?: string;
    strictComponentTypes?: boolean;
    format?: boolean;
    strictTokenTypes?: boolean;
    template?: TypingsTemplate;
    onError?: () => void;
}): Promise<void>;

export { generateThemeTypings };
