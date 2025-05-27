const HEADER_HEIGHT = 'var(--chakra-space-3) + var(--chakra-sizes-12)'
const HANDLE_PADDING = '12px'
const HANDLE_GAP = 0.5
const HANDLE_HEIGHT = 24

export const CANVAS_GRID = HANDLE_HEIGHT + HANDLE_GAP * 16

export const getHandlePosition = (index = 0) => {
  return `calc(${HEADER_HEIGHT} + ${HANDLE_PADDING} + ${index * HANDLE_HEIGHT}px + ${HANDLE_GAP * index}rem)`
}
