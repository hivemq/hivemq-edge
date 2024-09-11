import { FC, useMemo } from 'react'
import { useStore } from 'reactflow'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'

// TODO[NVL] ChakraUI Theme doesn't support ButtonGroup
const ToolbarButtonGroup: FC<ButtonGroupProps> = ({ children, ...rest }) => {
  const zoomFactor = useStore((s) => s.transform[2])

  const getToolbarSize = useMemo<string>(() => {
    if (zoomFactor >= 1.5) return 'lg'
    if (zoomFactor >= 1) return 'md'
    if (zoomFactor >= 0.75) return 'sm'
    return 'xs'
  }, [zoomFactor])

  return (
    <ButtonGroup
      size={getToolbarSize}
      variant="solid"
      colorScheme="gray"
      orientation="vertical"
      isAttached
      sx={{
        boxShadow: '0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19)',
        backgroundColor: 'rgba(0, 0, 0, 0.19)',
      }}
      {...rest}
    >
      {children}
    </ButtonGroup>
  )
}

export default ToolbarButtonGroup
