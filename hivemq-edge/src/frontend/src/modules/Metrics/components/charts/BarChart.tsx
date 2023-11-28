import { FC, useEffect, useState } from 'react'
import { BarDatum, ResponsiveBar } from '@nivo/bar'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'
import { Box } from '@chakra-ui/react'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'

interface BarChartProps {
  metricName: string
  'aria-label': string
}

interface Datum extends BarDatum {
  sampleTime: string
  count: number
}

const MAX_SERIES = 10

const BarChart: FC<BarChartProps> = ({ metricName, 'aria-label': ariaLabel }) => {
  const { data } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])
  const { t } = useTranslation()

  useEffect(() => {
    if (!data) return

    setSeries((old) => {
      const newTime: DataPoint = {
        value: data.value as number,
        sampleTime: data.sampleTime,
      }
      const newSeries = [newTime, ...old]
      newSeries.length = Math.min(newSeries.length, MAX_SERIES)
      return newSeries
    })
  }, [data])

  if (!metricName) return null

  const barSeries: Datum[] = [...series]
    .reverse()
    .map((e) => ({ sampleTime: e.sampleTime as string, count: e.value as number }))

  return (
    <Box w={'100%'} h={250}>
      <ResponsiveBar
        data={barSeries}
        keys={['count']}
        indexBy="sampleTime"
        margin={{ top: 10, right: 80, bottom: 40, left: 40 }}
        padding={0.3}
        valueScale={{ type: 'linear' }}
        indexScale={{ type: 'band', round: true }}
        colors={{ scheme: 'nivo' }}
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
            return '+' + DateTime.fromISO(value).second
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
            dataFrom: 'keys',
            anchor: 'bottom-right',
            direction: 'column',
            justify: false,
            translateX: 120,
            translateY: 0,
            itemsSpacing: 2,
            itemWidth: 100,
            itemHeight: 20,
            itemDirection: 'left-to-right',
            itemOpacity: 0.85,
            symbolSize: 20,
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
