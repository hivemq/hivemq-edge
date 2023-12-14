import { FC } from 'react'
import { BarDatum, ResponsiveBar } from '@nivo/bar'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { Badge, Box, Card, Text, useTheme } from '@chakra-ui/react'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import { ChartProps } from '../../types.ts'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'

interface Datum extends BarDatum {
  sampleTime: string
}

const BarChart: FC<ChartProps> = ({ data, metricName, 'aria-label': ariaLabel, chartTheme, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()

  if (!metricName) return null

  const { suffix, device } = extractMetricInfo(metricName)
  const seriesName = t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')

  const barSeries: Datum[] = [...data]
    .reverse()
    .map((e) => ({ sampleTime: e.sampleTime as string, [seriesName]: e.value as number }))

  const colorScheme = chartTheme?.colourScheme || 'red'
  const colorElement = colors[colorScheme][500]

  return (
    <Box w={'100%'} {...props}>
      <ResponsiveBar
        data={barSeries}
        keys={[seriesName]}
        indexBy="sampleTime"
        valueScale={{ type: 'linear' }}
        indexScale={{ type: 'band', round: true }}
        colors={[colorElement]}
        tooltip={(d) => (
          <Card p={1} data-testid={'bar-chart-tooltip'}>
            <Badge backgroundColor={d.color} color={'white'}>
              {d.id}
            </Badge>
            <DateTimeRenderer date={DateTime.fromISO(d.indexValue as string)} isShort />
            <Text fontWeight={'bold'}>{d.formattedValue}</Text>
          </Card>
        )}
        // defs={[
        //   {
        //     id: 'dots',
        //     type: 'patternDots',
        //     background: 'inherit',
        //     color: '#38bcb2',
        //     size: 4,
        //     padding: 1,
        //     stagger: true,
        //   },
        //   {
        //     id: 'lines',
        //     type: 'patternLines',
        //     background: 'inherit',
        //     color: '#eed312',
        //     rotation: -45,
        //     lineWidth: 6,
        //     spacing: 10,
        //   },
        // ]}
        // fill={[
        //   {
        //     match: {
        //       id: 'fries',
        //     },
        //     id: 'dots',
        //   },
        //   {
        //     match: {
        //       id: 'sandwich',
        //     },
        //     id: 'lines',
        //   },
        // ]}
        borderColor={{
          from: 'color',
          modifiers: [['darker', 1.6]],
        }}
        axisTop={null}
        axisRight={null}
        axisBottom={{
          tickSize: 5,
          tickPadding: 5,
          tickRotation: 0,
          legend: t('metrics.charts.LineChart.ariaLabel.legend'),
          legendPosition: 'middle',
          legendOffset: 32,
          truncateTickAt: 0,
          format: (value) => {
            const duration = DateTime.fromISO(value).diffNow(['second'])
            const rescaledDuration = duration
              .negate()
              .mapUnits((x) => Math.floor(x))
              .rescale()
            return rescaledDuration.as('seconds')
          },
        }}
        axisLeft={{
          tickSize: 5,
          tickPadding: 5,
          tickRotation: 0,
          // legend: 'count',
          legendPosition: 'middle',
          legendOffset: -40,
          truncateTickAt: 0,
        }}
        labelSkipWidth={12}
        labelSkipHeight={12}
        labelTextColor={{
          from: 'color',
          modifiers: [['darker', 1.6]],
        }}
        legends={[
          {
            direction: 'row',
            anchor: 'bottom-right',
            dataFrom: 'keys',
            itemDirection: 'right-to-left',
            translateY: 70,

            itemWidth: 0,
            itemHeight: 20,
            // itemOpacity: 0.85,
            // symbolSize: 20,
            effects: [
              {
                on: 'hover',
                style: {
                  itemOpacity: 1,
                },
              },
            ],
          },
        ]}
        role="application"
        ariaLabel={ariaLabel}
        // TODO[NVL]: Cannot have aria-label without a role (a11y)
        // barAriaLabel={(e) => e.id + ': ' + e.formattedValue + ' in country: ' + e.indexValue}
      />
    </Box>
  )
}

export default BarChart
