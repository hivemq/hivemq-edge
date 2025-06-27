/**
 * Utility functions for handling dynamic base paths in reverse proxy scenarios
 */

/**
 * Detects the base path from the current URL
 * Examples:
 * - /app -> '' (direct access)
 * - /gnarf/app -> '/gnarf' (proxied access)
 * - /some/proxy/path/app -> '/some/proxy/path' (any proxy path)
 */
export function detectBasePath(): string {
  const pathname = window.location.pathname
  
  // If pathname ends with /app, extract everything before it
  if (pathname.endsWith('/app')) {
    const basePath = pathname.slice(0, -4) // Remove '/app'
    return basePath
  }
  
  // If pathname contains /app/ somewhere, extract everything before /app
  const appIndex = pathname.indexOf('/app/')
  if (appIndex !== -1) {
    return pathname.slice(0, appIndex)
  }
  
  // If pathname is exactly /app, no base path
  if (pathname === '/app') {
    return ''
  }
  
  // Default case - no base path detected
  return ''
}

/**
 * Gets the full base path including /app for router basename
 */
export function getRouterBasename(): string {
  const basePath = detectBasePath()
  return basePath + '/app'
}

/**
 * Gets the API base URL with the detected base path
 */
export function getApiBaseUrl(): string {
  const basePath = detectBasePath()
  return basePath || ''
}

/**
 * Corrects asset paths to include the detected base path
 */
export function correctAssetPath(originalPath: string): string {
  const basePath = detectBasePath()
  
  // If no base path or path already includes base path, return as is
  if (!basePath || originalPath.startsWith(basePath)) {
    return originalPath
  }
  
  // Add base path to the beginning of the asset path
  return basePath + originalPath
}
