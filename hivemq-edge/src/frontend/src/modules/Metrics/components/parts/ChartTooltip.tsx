import { FC } from 'react'
import { Badge, Card, Text } from '@chakra-ui/react'
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
      <Badge backgroundColor={color} color={'white'}>
        {id}
      </Badge>
      <DateTimeRenderer date={date} isShort />
      <Text fontWeight={'bold'}>{formattedValue}</Text>
    </Card>
  )
}

export default ChartTooltip
