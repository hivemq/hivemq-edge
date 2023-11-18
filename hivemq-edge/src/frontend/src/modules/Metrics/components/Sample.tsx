import { FC, useEffect, useMemo, useState } from 'react'
import {
  Box,
  CloseButton,
  HStack,
  Spinner,
  Stat,
  StatArrow,
  StatHelpText,
  StatLabel,
  StatNumber,
  Text,
  VStack,
} from '@chakra-ui/react'
import { NotAllowedIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

import { extractMetricInfo } from '../utils/metrics-name.utils.ts'

interface SampleProps {
  metricName?: string
  onClose?: () => void
}

const MAX_SERIES = 10

const diff = (current: number, previous: number) => current - previous

const Sample: FC<SampleProps> = ({ metricName, onClose }) => {
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

  const formatNumber = Intl.NumberFormat(navigator.language)

  const { suffix, id } = extractMetricInfo(metricName)
  return (
    <Stat variant="hivemq">
      <StatLabel isTruncated>
        <HStack alignItems={'flex-start'}>
          <VStack flex={1} overflowX={'hidden'} gap={0} alignItems={'flex-start'}>
            <Text textOverflow={'ellipsis'}>{t(`metrics.protocolAdapters.${suffix}`).replaceAll('.', ' ')}</Text>
            <Text>{id}</Text>
          </VStack>
          <Box>
            <CloseButton aria-label={'Remove from panel'} size={'sm'} onClick={onClose} />
          </Box>
        </HStack>
      </StatLabel>
      <StatNumber py={2}>
        {isLoading && <Spinner />}
        {!!error && <NotAllowedIcon color="red.100" />}
        {formatNumber.format(series[0]?.value as number)}
      </StatNumber>
      {!!change && (
        <StatHelpText>
          <StatArrow type={change > 0 ? 'increase' : 'decrease'} />
          {formatNumber.format(Math.abs(change))}
        </StatHelpText>
      )}
    </Stat>
  )
}

export default Sample
