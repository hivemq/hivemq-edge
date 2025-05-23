/**
 * Extract color scheme names
 * by validating that every property of type ColorHue is in the object
 */
declare function extractColorSchemeTypes(theme: Record<string, unknown>): string[];

export { extractColorSchemeTypes };
