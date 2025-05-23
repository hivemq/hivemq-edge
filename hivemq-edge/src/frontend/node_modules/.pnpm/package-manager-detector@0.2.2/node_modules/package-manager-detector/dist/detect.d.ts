import { D as DetectOptions, d as DetectResult } from './shared/package-manager-detector.ca2fb30b.js';

/**
 * Detects the package manager used in the project.
 * @param options {DetectOptions} The options to use when detecting the package manager.
 * @returns {Promise<DetectResult | null>} The detected package manager or `null` if not found.
 */
declare function detect(options?: DetectOptions): Promise<DetectResult | null>;
/**
 * Detects the package manager used in the project.
 * @param options {DetectOptions} The options to use when detecting the package manager.
 * @returns {DetectResult | null>} The detected package manager or `null` if not found.
 */
declare function detectSync(options?: DetectOptions): DetectResult | null;

export { detect, detectSync };
