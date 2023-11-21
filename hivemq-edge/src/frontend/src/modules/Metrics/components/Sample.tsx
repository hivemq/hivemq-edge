import { FC, useEffect, useState } from 'react'
import { Box, CloseButton, Icon, IconButton, VStack } from '@chakra-ui/react'
import { LuClipboardCopy } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

import { extractMetricInfo } from '../utils/metrics-name.utils.ts'
import SampleRenderer from '@/modules/Metrics/components/SampleRenderer.tsx'

interface SampleProps {
  metricName?: string
  onClose?: () => void
  onClipboardCopy?: (metricName: string, timestamp: string, value: number) => void
}

const MAX_SERIES = 10

const Sample: FC<SampleProps> = ({ metricName, onClose, onClipboardCopy }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])
  const [isMajor] = useState(false)

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

  const metricInfo = extractMetricInfo(metricName)
  return (
    <SampleRenderer
      metricInfo={metricInfo}
      isLoading={isLoading}
      error={error}
      series={series}
      {...(isMajor ? { gridColumn: '1 / span 2' } : {})}
    >
      <VStack>
        <Box flex={1}>
          <CloseButton aria-label={t('metrics.command.remove.ariaLabel') as string} size={'sm'} onClick={onClose} />
        </Box>
        <Box>
          <IconButton
            size={'xs'}
            variant={'ghost'}
            icon={<Icon as={LuClipboardCopy} fontSize={'16px'} />}
            aria-label={t('metrics.command.copy.ariaLabel')}
            onClick={() => onClipboardCopy?.(metricName, series[0]?.sampleTime as string, series[0]?.value as number)}
          />
        </Box>
      </VStack>
    </SampleRenderer>
  )
}

export default Sample
