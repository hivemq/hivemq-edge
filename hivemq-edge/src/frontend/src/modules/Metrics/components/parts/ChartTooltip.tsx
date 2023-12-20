import { FC } from 'react'
import { Card, HStack, Square, Text } from '@chakra-ui/react'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import { DateTime } from 'luxon'
import { PointTooltipProps } from '@nivo/line'
import { BarTooltipProps } from '@nivo/bar'

interface TooltipProps {
  d?: PointTooltipProps
  f?: BarTooltipProps<unknown>

  color: string
  id: string | number
  date: DateTime
  formattedValue: string
}

const ChartTooltip: FC<TooltipProps> = ({ formattedValue, color, date, id }) => {
  return (
    <Card p={1} data-testid={'chart-tooltip'}>
      <HStack data-testid={'chart-tooltip-id'}>
        <Square size={4} bg={color} />
        <Text>{id}</Text>
      </HStack>

      <DateTimeRenderer date={date} isShort />
      <Text fontWeight={'bold'} data-testid={'chart-tooltip-value'}>
        {formattedValue}
      </Text>
    </Card>
  )
}

export default ChartTooltip
