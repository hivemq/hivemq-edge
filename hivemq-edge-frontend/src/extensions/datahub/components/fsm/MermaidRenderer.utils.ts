import type { ColorMode, WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'
import type { FsmState } from '@datahub/types.ts'
import type { FsmTransition } from '@datahub/types.ts'

/**
 * Helper to resolve color token to hex
 * @param colorPath
 * @param theme
 * @param colorMode
 */
export const resolveColor = (colorPath: string, theme: WithCSSVar<Dict>, colorMode: ColorMode): string => {
  // Get Chakra UI theme colors for current mode
  const { colors } = theme

  const parts = colorPath.split('.')
  let value: unknown = colors

  for (const part of parts) {
    if (value && typeof value === 'object' && part in value) {
      value = (value as Record<string, unknown>)[part]
    } else {
      value = undefined
      break
    }
  }

  // If it's an object with _light/_dark, use colorMode
  if (typeof value === 'object' && value !== null) {
    const modeKey = colorMode === 'dark' ? '_dark' : '_light'
    const modeValue = (value as Record<string, unknown>)[modeKey]
    if (typeof modeValue === 'string') {
      return resolveColor(modeValue.replace('colors.', ''), theme, colorMode)
    }
  }

  return typeof value === 'string' ? value : '#000000'
}

/**
 * Get Badge colors (matching TransitionSelect - SUBTLE variant is default)
 * @param colorScheme
 * @param theme
 * @param colorMode
 */
export const getBadgeColors = (colorScheme: string, theme: WithCSSVar<Dict>, colorMode: ColorMode) => {
  const isDark = colorMode === 'dark'

  // Subtle variant: light bg with dark text (light mode), transparent bg with light text (dark mode)
  switch (colorScheme) {
    case 'blue':
      return {
        fill: isDark ? resolveColor('blue.900', theme, colorMode) : resolveColor('blue.100', theme, colorMode),
        stroke: isDark ? resolveColor('blue.300', theme, colorMode) : resolveColor('blue.200', theme, colorMode),
        text: isDark ? resolveColor('blue.200', theme, colorMode) : resolveColor('blue.800', theme, colorMode),
      }
    case 'green':
      return {
        fill: isDark ? resolveColor('green.900', theme, colorMode) : resolveColor('green.100', theme, colorMode),
        stroke: isDark ? resolveColor('green.300', theme, colorMode) : resolveColor('green.200', theme, colorMode),
        text: isDark ? resolveColor('green.200', theme, colorMode) : resolveColor('green.800', theme, colorMode),
      }
    case 'red':
      return {
        fill: isDark ? resolveColor('red.900', theme, colorMode) : resolveColor('red.100', theme, colorMode),
        stroke: isDark ? resolveColor('red.300', theme, colorMode) : resolveColor('red.200', theme, colorMode),
        text: isDark ? resolveColor('red.200', theme, colorMode) : resolveColor('red.800', theme, colorMode),
      }
    case 'gray':
    default:
      return {
        fill: isDark ? resolveColor('gray.900', theme, colorMode) : resolveColor('gray.100', theme, colorMode),
        stroke: isDark ? resolveColor('gray.300', theme, colorMode) : resolveColor('gray.200', theme, colorMode),
        text: isDark ? resolveColor('gray.200', theme, colorMode) : resolveColor('gray.800', theme, colorMode),
      }
  }
}

// Helper to get CSS class for state type
export const getStateClass = (stateType: FsmState.Type): string => {
  switch (stateType) {
    case 'INITIAL':
      return 'initial'
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'failed'
    case 'INTERMEDIATE':
    default:
      return 'intermediate'
  }
}

// Helper to format transition label
export const getTransitionLabel = (transition: FsmTransition): string => {
  const guards = (transition as FsmTransition & { guards?: string }).guards
  return guards ? `${transition.event}<br/>+ ${guards}` : transition.event
}
