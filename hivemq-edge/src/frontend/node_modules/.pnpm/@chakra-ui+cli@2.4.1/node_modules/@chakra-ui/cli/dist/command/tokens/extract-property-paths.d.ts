/**
 * @example
 * { colors: ['red.500', 'green.500'] } => `colors: "red.500" | "green.500"`
 */
declare function printUnionMap(unions: Record<string, string[]>, strict?: boolean): string;
/**
 * Extract recursively all property paths with a max depth
 */
declare function extractPropertyPaths(target: unknown, maxDepth?: number): string[];

export { extractPropertyPaths, printUnionMap };
