import { FC } from 'react'
import { Badge, type BadgeProps } from '@chakra-ui/react'

import { RangeOption } from '../types.ts'
import { useRangeTranslation } from '../hooks/useRangeTranslation.ts'

interface OptionBadgeProps extends BadgeProps {
  data: RangeOption
}

const OptionBadge: FC<OptionBadgeProps> = ({ data }) => {
  const { translateBadgeFrom } = useRangeTranslation()
  const badge = translateBadgeFrom(data)

  if (!badge) return null

  return (
    <Badge
      as={'p'}
      textAlign={'center'}
      textTransform={'lowercase'}
      size={'sm'}
      data-testid={`dateRange-option-badge-${data.value}`}
      data-group={data.colorScheme}
      variant="solid"
      colorScheme={data.colorScheme}
      mr={2}
    >
      {badge}
    </Badge>
  )
}

export default OptionBadge
