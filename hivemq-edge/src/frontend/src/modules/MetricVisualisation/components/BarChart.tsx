import { FC, useEffect, useState } from 'react'
import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

interface BarChartProps {
  metricName: string
}

const MAX_SERIES = 10

const BarChart: FC<BarChartProps> = ({ metricName }) => {
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

  return <div></div>
}

export default BarChart
