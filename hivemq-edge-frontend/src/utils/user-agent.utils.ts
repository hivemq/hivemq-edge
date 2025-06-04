/* istanbul ignore file -- @preserve */
export const PLATFORM_MACOS = 'MacOS'
export const PLATFORM_OTHERS = 'Others'

/**
 * TODO[NVL] This is unsafe, navigator.platform is deprecated
 */
export const getUserAgentPlatform = () => {
  const { platform, userAgent } = navigator
  let os = platform
  if (!os) {
    os = userAgent
  }
  os = os.toLowerCase()
  return os.includes('mac') ? PLATFORM_MACOS : PLATFORM_OTHERS
}
