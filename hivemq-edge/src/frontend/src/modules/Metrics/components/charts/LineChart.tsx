import { FC } from 'react'
import { ResponsiveLine } from '@nivo/line'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { Box, useTheme } from '@chakra-ui/react'

import { ChartProps } from '../../types.ts'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import ChartTooltip from '../parts/ChartTooltip.tsx'

const LineChart: FC<ChartProps> = ({ data, metricName, 'aria-label': ariaLabel, chartTheme, ...props }) => {
  const { t } = useTranslation()
  const { colors } = useTheme()

  if (!metricName) return null

  const boundaries = {
    x: Math.min(...data.map((e) => e.value as number)),
    y: Math.max(...data.map((e) => e.value as number)),
  }

  const { suffix, device, id } = extractMetricInfo(metricName)
  let seriesName = t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')
  seriesName = `${seriesName} - ${id}`

  const colorScheme = chartTheme?.colourScheme || 'red'
  const colorElement = colors[colorScheme][500]

  return (
    <Box w={'100%'} {...props} role={'application'} aria-label={ariaLabel}>
      <ResponsiveLine
        useMesh
        // TODO[NVL] ResponsiveLine doesn't support aria-label; adding it to the wrapper element
        role={'none'}
        data={[
          {
            id: seriesName,
            color: 'red',
            data: [...data]
              .reverse()
              .map((e) => ({ x: DateTime.fromISO(e.sampleTime as string).toMillis(), y: e.value })),
          },
        ]}
        // colors={{ scheme: 'nivo' }}
        colors={[colorElement]}
        margin={{ top: 0, right: 10, bottom: 70, left: 50 }}
        yScale={{ type: 'linear', min: boundaries.x, max: boundaries.y, stacked: true }}
        // xScale={{ type: 'linear' }}
        // yScale={{ type: 'linear', stacked: true, min: 0, max: 2500 }}
        // yFormat=" >-.2f"
        curve="monotoneX"
        // axisTop={null}
        // axisRight={{
        //   tickValues: [0, 500, 1000, 1500, 2000, 2500],
        //   tickSize: 5,
        //   tickPadding: 5,
        //   tickRotation: 0,
        //   format: '.2s',
        //   legend: '',
        //   legendOffset: 0,
        // }}
        axisBottom={{
          // tickValues: [0, 20, 40, 60, 80, 100, 120],
          // tickSize: 5,
          // tickPadding: 5,
          // tickRotation: 0,
          // // format: '.2f',
          format: (value) => {
            const duration = DateTime.fromMillis(value).diffNow(['second'])
            const rescaledDuration = duration
              .negate()
              .mapUnits((x) => Math.floor(x))
              .rescale()
            return rescaledDuration.as('seconds')
          },
          legend: t('metrics.charts.LineChart.ariaLabel.legend'),
          legendOffset: 36,
          legendPosition: 'middle',
        }}
        // axisLeft={{
        //   tickValues: [0, 500, 1000, 1500, 2000, 2500],
        //   tickSize: 5,
        //   tickPadding: 5,
        //   tickRotation: 0,
        //   format: '.2s',
        //   legend: 'volume',
        //   legendOffset: -40,
        //   legendPosition: 'middle',
        // }}
        // enableGridX={false}
        // colors={{ scheme: 'spectral' }}
        // lineWidth={1}
        // pointSize={4}
        // pointColor={{ theme: 'background' }}
        // pointBorderWidth={1}
        // pointBorderColor={{ from: 'serieColor' }}
        // pointLabelYOffset={-12}
        // useMesh={true}
        // gridXValues={[0, 20, 40, 60, 80, 100, 120]}
        // gridYValues={[0, 500, 1000, 1500, 2000, 2500]}
        // legends={[
        //   {
        //     anchor: 'bottom-right',
        //     direction: 'column',
        //     justify: false,
        //     translateX: 140,
        //     translateY: 0,
        //     itemsSpacing: 2,
        //     itemDirection: 'left-to-right',
        //     itemWidth: 80,
        //     itemHeight: 12,
        //     itemOpacity: 0.75,
        //     symbolSize: 12,
        //     symbolShape: 'circle',
        //     symbolBorderColor: 'rgba(0, 0, 0, .5)',
        //     effects: [
        //       {
        //         on: 'hover',
        //         style: {
        //           itemBackground: 'rgba(0, 0, 0, .03)',
        //           itemOpacity: 1,
        //         },
        //       },
        //     ],
        //   },
        // ]}

        tooltip={(d) => (
          <ChartTooltip
            color={d.point.serieColor}
            id={d.point.serieId}
            date={DateTime.fromMillis(d.point.data.x as number)}
            formattedValue={d.point.data.yFormatted.toString()}
          />
        )}
        legends={[
          {
            direction: 'row',
            anchor: 'bottom-right',
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
      />
    </Box>
  )
}

export default LineChart
