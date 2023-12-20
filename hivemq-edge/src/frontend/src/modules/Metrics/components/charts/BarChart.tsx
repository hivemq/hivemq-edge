import { FC } from 'react'
import { Theme } from '@nivo/core'
import { BarDatum, ResponsiveBar } from '@nivo/bar'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { Box, useTheme } from '@chakra-ui/react'

import { ChartProps } from '../../types.ts'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import ChartTooltip from '../parts/ChartTooltip.tsx'

interface Datum extends BarDatum {
  sampleTime: string
}

const BarChart: FC<ChartProps> = ({ data, metricName, 'aria-label': ariaLabel, chartTheme, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()
  const nivoTheme: Theme = {
    text: {
      fill: 'var(--chakra-colors-chakra-body-text)',
    },
  }

  if (!metricName) return null

  const { suffix, device, id } = extractMetricInfo(metricName)
  let seriesName = t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')
  seriesName = `${seriesName} - ${id}`

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
        // tooltip={(d) => (
        //   <Card p={1} data-testid={'bar-chart-tooltip'}>
        //     <Badge backgroundColor={d.color} color={'white'}>
        //       {d.id}
        //     </Badge>
        //     <DateTimeRenderer date={DateTime.fromISO(d.indexValue as string)} isShort />
        //     <Text fontWeight={'bold'}>{d.formattedValue}</Text>
        //   </Card>
        // )}
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
          modifiers: [['darker', 0]],
        }}
        margin={{ top: 5, right: 0, bottom: 70, left: 40 }}
        padding={0.3}
        tooltip={(d) => (
          <ChartTooltip
            color={d.color}
            id={d.id}
            date={DateTime.fromISO(d.indexValue as string)}
            formattedValue={d.formattedValue.toString()}
          />
        )}
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
        theme={nivoTheme}
      />
    </Box>
  )
}

export default BarChart
