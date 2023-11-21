import { FC, ReactNode, useMemo } from 'react'
import {
  HStack,
  Spinner,
  Stat,
  StatArrow,
  StatHelpText,
  StatLabel,
  StatNumber,
  StatProps,
  Text,
  Tooltip,
} from '@chakra-ui/react'
import { NotAllowedIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ApiError, DataPoint } from '@/api/__generated__'
import { MetricInfo } from '../utils/metrics-name.utils.ts'

interface SampleRendererProps extends StatProps {
  metricInfo: MetricInfo
  series: DataPoint[]
  isLoading: boolean
  error?: ApiError | null
  children?: ReactNode
}

const SampleRenderer: FC<SampleRendererProps> = ({ metricInfo, series, isLoading, error, children, ...props }) => {
  const { t } = useTranslation()
  const { device, suffix, id } = metricInfo
  const formatNumber = Intl.NumberFormat(navigator.language)

  const diff = (current: number, previous: number) => current - previous

  const change = useMemo(() => {
    if (series.length < 2) return undefined
    const d = diff(series[0].value as number, series[1].value as number)
    return Number.isFinite(d) ? d : undefined
  }, [series])

  return (
    <HStack alignItems={'flex-start'} gap={0}>
      <Stat variant="hivemq" {...props} overflowX={'hidden'} h={'100%'}>
        <StatLabel isTruncated h={'100%'}>
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
      </Stat>
      {children}
    </HStack>
  )
}

export default SampleRenderer
