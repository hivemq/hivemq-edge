import type { FC } from 'react'
import { useEffect, useState } from 'react'
import type { StackProps } from '@chakra-ui/react'
import { Box, CloseButton, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.ts'
import type { DataPoint } from '@/api/__generated__'

import ClipboardCopyIconButton from '@/components/Chakra/ClipboardCopyIconButton.tsx'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import SampleRenderer from '../charts/SampleRenderer.tsx'
import type { ChartTheme } from '@/modules/Metrics/types.ts'

interface SampleProps extends StackProps {
  metricName?: string
  chartTheme?: ChartTheme
  onClose?: () => void
  canEdit?: boolean
}

const MAX_SERIES = 10

const Sample: FC<SampleProps> = ({ metricName, chartTheme, onClose, canEdit = true, ...props }) => {
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
      chartTheme={chartTheme}
      {...(isMajor ? { gridColumn: '1 / span 2' } : {})}
      {...props}
    >
      <VStack ml={1}>
        {canEdit && (
          <Box flex={1}>
            <CloseButton
              data-testid="metrics-remove"
              aria-label={t('metrics.command.remove.ariaLabel')}
              size="sm"
              onClick={onClose}
            />
          </Box>
        )}
        <Box>
          <ClipboardCopyIconButton content={metricName} />
        </Box>
      </VStack>
    </SampleRenderer>
  )
}

export default Sample
