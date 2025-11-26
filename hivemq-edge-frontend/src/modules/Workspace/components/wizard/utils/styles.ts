/**
 * GHOST-related colors
 * TODO[NVL] These must be integrated in the theme
 */
export const GHOST_COLOR_BACKGROUND = '#EBF8FF'
export const GHOST_COLOR_EDGE = '#4299E1'
export const GHOST_SUCCESS_SHADOW = '0 0 0 4px rgba(72, 187, 120, 0.6), 0 0 20px rgba(72, 187, 120, 0.4)'
export const GHOST_SUCCESS_TRANSITION = 'box-shadow 0.3s ease-in'
export const GHOST_SUCCESS_BOX_SHADOW_TRANSITION = 'box-shadow 0.5s ease-out'
export const GHOST_SUCCESS_OPACITY_TRANSITION = 'opacity 0.5s ease-out'

/**
 * Enhanced ghost node styling with glowing effect
 */
export const GHOST_STYLE_ENHANCED = {
  opacity: 0.75,
  border: `3px dashed ${GHOST_COLOR_EDGE}`,
  backgroundColor: GHOST_COLOR_BACKGROUND,
  boxShadow: '0 0 0 4px rgba(66, 153, 225, 0.4), 0 0 20px rgba(66, 153, 225, 0.6)',
  pointerEvents: 'none' as const,
  transition: 'all 0.3s ease',
}

/**
 * Selectable ghost node styling - allows clicking to see edge highlighting
 */
export const GHOST_STYLE_SELECTABLE = {
  ...GHOST_STYLE_ENHANCED,
  pointerEvents: 'all' as const, // Allow interaction
  cursor: 'pointer' as const,
}

/**
 * Ghost edge styling
 */
export const GHOST_EDGE_STYLE = {
  stroke: GHOST_COLOR_EDGE,
  strokeWidth: 2,
  strokeDasharray: '5,5',
  opacity: 0.6,
}

/**
 * Legacy ghost node styling (for backward compatibility)
 */
export const GHOST_STYLE = {
  opacity: 0.6,
  border: `2px dashed ${GHOST_COLOR_EDGE}`,
  backgroundColor: GHOST_COLOR_BACKGROUND,
  pointerEvents: 'none' as const,
}
