import TooltipBadge from '@/components/Chakra/TooltipBadge.tsx'
import type { IconProps, ComponentWithAs } from '@chakra-ui/react'
import { Icon } from '@chakra-ui/react'

const TooltipIcon: ComponentWithAs<'svg', IconProps> = (props) => {
  return (
    <TooltipBadge aria-label={props['aria-label']}>
      <Icon {...props} verticalAlign="bottom" boxSize={4} />
    </TooltipBadge>
  )
}

export default TooltipIcon
