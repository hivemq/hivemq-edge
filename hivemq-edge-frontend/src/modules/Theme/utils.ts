// TODO[NVL] Maybe a little complicated for a marginal improvement on border style. Convert to a ChakraUI theme anyway
export const getDropZoneBorder = (color: string) => {
  return {
    bgGradient: `repeating-linear(0deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(90deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(180deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(270deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px)`,
    backgroundSize: '2px 100%, 100% 2px, 2px 100% , 100% 2px',
    backgroundPosition: '0 0, 0 0, 100% 0, 0 100%',
    backgroundRepeat: 'no-repeat',
    borderRadius: '4px',
  }
}

export const ANIMATION = {
  FIT_VIEW_DURATION_MS: 500,
}

export const Z_INDICES = {
  // Must be higher than the modal overlay (--chakra-zIndices-modal)
  ACCESSIBLE_DRAG_N_DROP: 1450,
}
