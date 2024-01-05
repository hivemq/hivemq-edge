import { FC } from 'react'
import { IconButtonProps, TooltipProps, Tooltip, IconButton as CuiIconButton } from '@chakra-ui/react'

interface IconTooltipButtonProps extends Omit<IconButtonProps, 'icon'> {
  icon: React.ReactElement
  tooltipProps?: Omit<TooltipProps, 'aria-label' | 'hasArrow' | 'children'>
}

const IconButton: FC<IconTooltipButtonProps> = ({ 'aria-label': ariaLabel, tooltipProps, ...props }) => {
  return (
    <Tooltip label={ariaLabel} placement={'top'} {...tooltipProps} hasArrow>
      <CuiIconButton aria-label={ariaLabel} {...props} />
    </Tooltip>
  )
}

export default IconButton
