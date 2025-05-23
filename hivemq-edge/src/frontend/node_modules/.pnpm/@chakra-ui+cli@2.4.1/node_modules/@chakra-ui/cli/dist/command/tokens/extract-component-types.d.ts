interface ComponentType extends Record<string, string[]> {
    sizes: string[];
    variants: string[];
}
declare function extractComponentTypes(theme: Record<string, unknown>): Record<string, ComponentType>;
declare function printComponentTypes(componentTypes: Record<string, ComponentType>, strict?: boolean): string;

export { extractComponentTypes, printComponentTypes };
