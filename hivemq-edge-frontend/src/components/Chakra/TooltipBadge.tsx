import type { BadgeProps } from '@chakra-ui/react'
import { Badge, Tooltip } from '@chakra-ui/react'
import type { FC } from 'react'

const TooltipBadge: FC<BadgeProps> = (props) => {
  return (
    <Tooltip label={props['aria-label']} hasArrow placement="top">
      <Badge tabIndex={0} {...props}>
        {props.children}
      </Badge>
    </Tooltip>
  )
}

export default TooltipBadge
