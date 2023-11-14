import { FC } from 'react'
import { Text, Tooltip, type TextProps } from '@chakra-ui/react'
import { DateTime } from 'luxon'

import { toHuman } from './utils/duration.utils.ts'

interface DateTimeRendererProps extends TextProps {
  date: DateTime
  isApprox?: boolean
}

const DateTimeRenderer: FC<DateTimeRendererProps> = ({ date, isApprox = false, ...props }) => {
  const formatLongDate = new Intl.DateTimeFormat(navigator.language, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
    fractionalSecondDigits: 3,
  })

  if (isApprox)
    return (
      <Tooltip
        data-testid={'date-time-tooltip'}
        hasArrow
        label={formatLongDate.format(date.toJSDate())}
        placement="top"
        maxW={'200px'}
      >
        <Text data-testid={'date-time-approx'} width={'fit-content'} {...props}>
          {toHuman(date)}
        </Text>
      </Tooltip>
    )

  return (
    <Text data-testid={'date-time-full'} {...props}>
      {formatLongDate.format(date.toJSDate())}
    </Text>
  )
}

export default DateTimeRenderer
