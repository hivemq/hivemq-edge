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
  useTheme,
} from '@chakra-ui/react'
import { NotAllowedIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ApiError, DataPoint } from '@/api/__generated__'
import { MetricInfo } from '../../utils/metrics-name.utils.ts'
import { ChartTheme } from '@/modules/Metrics/types.ts'

interface SampleRendererProps extends StatProps {
  metricInfo: MetricInfo
  series: DataPoint[]
  isLoading: boolean
  error?: ApiError | null
  children?: ReactNode
  chartTheme?: ChartTheme
}

const SampleRenderer: FC<SampleRendererProps> = ({
  metricInfo,
  series,
  chartTheme,
  isLoading,
  error,
  children,
  ...props
}) => {
  const { t } = useTranslation()
  const { device, suffix, id } = metricInfo
  const formatNumber = Intl.NumberFormat(navigator.language)
  const { colors } = useTheme()
  const { role, ...rest } = props

  const diff = (current: number, previous: number) => current - previous

  const change = useMemo(() => {
    if (series.length < 2) return undefined
    const d = diff(series[0].value as number, series[1].value as number)
    return Number.isFinite(d) ? d : undefined
  }, [series])

  const n = series[0]?.value as number

  const colorScheme = chartTheme?.colourScheme || 'red'
  const colorElement = colors[colorScheme][500]

  return (
    <HStack alignItems={'flex-start'} gap={0} role={role} aria-labelledby={`sample-title-${id}-${suffix}`}>
      <Stat
        variant="hivemq"
        {...rest}
        overflowX={'hidden'}
        h={'100%'}
        sx={{
          borderColor: colorElement,
          "dd[data-testid='metric-value']": {
            color: colorElement,
          },
        }}
      >
        <StatLabel isTruncated h={'100%'} id={`sample-title-${id}-${suffix}`}>
          <Tooltip label={t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')} placement={'top'}>
            <Text textOverflow={'ellipsis'}>{t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')}</Text>
          </Tooltip>
          <Text>{id}</Text>
        </StatLabel>

        <StatNumber py={2} data-testid={`metric-value`}>
          {isLoading && <Spinner data-testid={`metric-loader`} />}
          {!!error && <NotAllowedIcon color="red.100" />}
          {isNaN(n) && '-'}
          {!isNaN(n) && formatNumber.format(n)}
        </StatNumber>
        {!!change && (
          <StatHelpText data-testid={`metric-change`}>
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
