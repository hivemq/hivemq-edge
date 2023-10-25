import { FC, useEffect, useState } from 'react'
import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

import { ResponsiveLine } from '@nivo/line'
import { DateTime } from 'luxon'
import { Box } from '@chakra-ui/react'

interface LineChartProps {
  metricName: string
}

const MAX_SERIES = 10

const LineChart: FC<LineChartProps> = ({ metricName }) => {
  const { data } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])

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

  const boundaries = {
    x: Math.min(...series.map((e) => e.value as number)),
    y: Math.max(...series.map((e) => e.value as number)),
  }
  console.log('XXXX sample', series, metricName, boundaries)

  return (
    <Box w={'100%'} h={250}>
      <ResponsiveLine
        // data={series.map((e) => [{ id: 'ddd', color: 'red' data}])}
        data={[
          {
            id: 'ddd',
            color: 'red',
            data: [...series]
              .reverse()
              .map((e) => ({ x: DateTime.fromISO(e.sampleTime as string).toMillis(), y: e.value })),
          },
        ]}
        margin={{ top: 0, right: 10, bottom: 50, left: 50 }}
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
            return '+' + DateTime.fromMillis(value).second
          },
          legend: 'timestamp (s)',
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
      />
    </Box>
  )
}

export default LineChart
