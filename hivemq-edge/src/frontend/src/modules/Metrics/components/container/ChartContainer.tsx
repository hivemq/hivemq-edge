import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Card, CardBody, CloseButton, HStack, Icon, type StackProps, VStack } from '@chakra-ui/react'
import { BiCollapseHorizontal, BiExpandHorizontal } from 'react-icons/bi'

import { DataPoint } from '@/api/__generated__'
import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import ClipboardCopyIconButton from '@/components/Chakra/ClipboardCopyIconButton.tsx'

import { ChartTheme, ChartType } from '../../types.ts'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import LineChart from '../charts/LineChart.tsx'
import BarChart from '../charts/BarChart.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

interface ChartContainerProps extends StackProps {
  chartType: ChartType
  chartTheme?: ChartTheme
  metricName?: string
  onClose?: () => void
  canEdit?: boolean
}

const MAX_SERIES = 10

const ChartContainer: FC<ChartContainerProps> = ({
  chartType,
  chartTheme,
  metricName,
  onClose,
  canEdit = true,
  ...props
}) => {
  const { t } = useTranslation()
  const { data } = useGetSample(metricName)
  const [series, setSeries] = useState<DataPoint[]>([])
  const [gridSpan, setGridSpan] = useState(true)

  useEffect(() => {
    if (!data) return

    setSeries((old) => {
      const newTime: DataPoint = {
        value: data.value as number,
        sampleTime: data.sampleTime,
      }
      const newSeries = [newTime, ...old]
      newSeries.length = Math.min(newSeries.length, gridSpan ? MAX_SERIES : MAX_SERIES / 2)
      return newSeries
    })
  }, [data, gridSpan])

  if (!metricName) return null

  const metricInfo = extractMetricInfo(metricName)
  const { device, suffix } = metricInfo

  const Chart = chartType === ChartType.BAR_CHART ? BarChart : LineChart
  return (
    <HStack alignItems={'flex-start'} gap={0} {...props} gridColumn={gridSpan ? '1 / span 2' : 'auto'}>
      <Card size={'sm'} w={'100%'} data-testid={'chart-container'}>
        <CardBody h={180}>
          <Chart
            h={gridSpan ? 250 : 200}
            data={series}
            metricName={metricName}
            aria-label={t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')}
            chartTheme={chartTheme}
          />
        </CardBody>
      </Card>
      <VStack ml={1} data-testid={'chart-container-toolbar'}>
        {canEdit && (
          <>
            <Box flex={1}>
              <CloseButton
                data-testid="metrics-remove"
                aria-label={t('metrics.command.remove.ariaLabel') as string}
                size={'sm'}
                onClick={onClose}
              />
            </Box>
            <Box>
              <IconButton
                variant={'ghost'}
                colorScheme="gray"
                aria-label={gridSpan ? t('metrics.command.resize.collapse') : t('metrics.command.resize.expand')}
                size={'xs'}
                icon={<Icon as={gridSpan ? BiCollapseHorizontal : BiExpandHorizontal} fontSize={'1rem'} />}
                onClick={() => setGridSpan((old) => !old)}
              />
            </Box>
          </>
        )}
        <Box>
          <ClipboardCopyIconButton content={metricName} />
        </Box>
      </VStack>
    </HStack>
  )
}

export default ChartContainer
