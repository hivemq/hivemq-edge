import { FC, useEffect, useMemo, useState } from 'react'
import { Spinner, Stat, StatArrow, StatHelpText, StatLabel, StatNumber } from '@chakra-ui/react'
import { NotAllowedIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

interface SampleProps {
  metricName?: string
}

const MAX_SERIES = 10
const FRACTION_DIGITS = 1

const diff = (current: number, previous: number) => 100 * ((current - previous) / previous)

const Sample: FC<SampleProps> = ({ metricName }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])
  const change = useMemo(() => {
    if (series.length < 2) return undefined
    const d = diff(series[0].value as number, series[1].value as number)
    return Number.isFinite(d) ? d : undefined
  }, [series])

  useEffect(() => {
    if (data)
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

  // TODO[NVL] Not the best approach. Use props
  const splitMetricName = metricName.split('.')
  const [, , , , type, id] = splitMetricName.splice(0, 6)
  const suffix = splitMetricName.join('.')

  return (
    <Stat variant="hivemq">
      <StatLabel isTruncated>
        {type} - {id}
      </StatLabel>
      <StatLabel isTruncated>{t(`metrics.protocolAdapters.${suffix}`)}</StatLabel>
      <StatNumber>
        {isLoading && <Spinner />}
        {!!error && <NotAllowedIcon color="red.100" />}
        {series[0]?.value}
      </StatNumber>
      {!!change && (
        <StatHelpText>
          <StatArrow type={change > 0 ? 'increase' : 'decrease'} />
          {Math.abs(change).toFixed(FRACTION_DIGITS)}%
        </StatHelpText>
      )}
    </Stat>
  )
}

export default Sample
