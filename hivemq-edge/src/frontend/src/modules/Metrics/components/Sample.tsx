import { FC, useEffect, useMemo, useState } from 'react'
import {
  Box,
  CloseButton,
  HStack,
  Icon,
  IconButton,
  Spinner,
  Stat,
  StatArrow,
  StatHelpText,
  StatLabel,
  StatNumber,
  Text,
  Tooltip,
  VStack,
  StatProps,
} from '@chakra-ui/react'
import { LuClipboardCopy } from 'react-icons/lu'
import { NotAllowedIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import { DataPoint } from '@/api/__generated__'

import { extractMetricInfo } from '../utils/metrics-name.utils.ts'

interface SampleProps extends StatProps {
  metricName?: string
  onClose?: () => void
  onClipboardCopy?: (metricName: string, timestamp: string, value: number) => void
}

const MAX_SERIES = 10

const diff = (current: number, previous: number) => current - previous

const Sample: FC<SampleProps> = ({ metricName, onClose, onClipboardCopy, ...props }) => {
  const { t } = useTranslation()
  const { data, isLoading, error } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])
  const [isMajor] = useState(false)
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

  const { device, suffix, id } = extractMetricInfo(metricName)
  return (
    <Stat variant="hivemq" {...props} {...(isMajor ? { gridColumn: '1 / span 2' } : {})}>
      <HStack alignItems={'flex-start'}>
        <VStack flex={1} alignItems={'flex-start'} overflowX={'hidden'}>
          <StatLabel isTruncated>
            <Tooltip label={t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')} placement={'top'}>
              <Text textOverflow={'ellipsis'}>{t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')}</Text>
            </Tooltip>
            <Text>{id}</Text>
          </StatLabel>

          <StatNumber py={2}>
            {isLoading && <Spinner data-testid={`metric-loader`} />}
            {!!error && <NotAllowedIcon color="red.100" />}
            {formatNumber.format(series[0]?.value as number)}
          </StatNumber>
          {!!change && (
            <StatHelpText>
              <StatArrow type={change > 0 ? 'increase' : 'decrease'} />
              {formatNumber.format(Math.abs(change))}
            </StatHelpText>
          )}
        </VStack>
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
      </HStack>
    </Stat>
  )
}

export default Sample
