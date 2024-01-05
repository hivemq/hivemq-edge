import { BoxProps } from '@chakra-ui/react'
import { DataPoint } from '@/api/__generated__'

export enum ChartType {
  SAMPLE = 'SAMPLE',
  LINE_CHART = 'LINE_CHART',
  BAR_CHART = 'BAR_CHART',
}

export interface MetricNameOption {
  label: string
  value: string
  isDisabled?: boolean
}

export interface ChartTypeOption {
  label: string
  value: ChartType
}

export interface MetricDefinition {
  selectedTopic: MetricNameOption
  selectedChart?: ChartTypeOption
}

export interface MetricVisualisation {
  metricNames: string[]
  viewType: string
  label: string
  description?: string
}

export interface ChartProps extends BoxProps {
  metricName: string
  'aria-label': string
  data: DataPoint[]
  chartTheme?: ChartTheme
}

export interface ChartTheme {
  colourScheme: string
  deviceId?: string
}
