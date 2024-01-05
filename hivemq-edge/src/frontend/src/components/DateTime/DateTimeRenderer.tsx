import { FC } from 'react'
import { Text, Tooltip, type TextProps } from '@chakra-ui/react'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'

import { toHuman } from './utils/duration.utils.ts'

interface DateTimeRendererProps extends TextProps {
  date: DateTime
  isApprox?: boolean
  isShort?: boolean
}

const DateTimeRenderer: FC<DateTimeRendererProps> = ({ date, isApprox = false, isShort = false, ...props }) => {
  const { t } = useTranslation('components')
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

  const formatShortDate = new Intl.DateTimeFormat(navigator.language, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
  })

  const formatter = isShort ? formatShortDate : formatLongDate

  if (isApprox) {
    const relative = toHuman(date)
    return (
      <Tooltip
        data-testid={'date-time-tooltip'}
        hasArrow
        label={formatter.format(date.toJSDate())}
        placement="top"
        maxW={'200px'}
      >
        <Text data-testid={'date-time-approx'} width={'fit-content'} {...props}>
          {relative || t('DateTimeRenderer.seconds', { context: 'minus' })}
        </Text>
      </Tooltip>
    )
  }

  return (
    <Text data-testid={'date-time-full'} {...props}>
      {formatter.format(date.toJSDate())}
    </Text>
  )
}

export default DateTimeRenderer
