import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Avatar,
  Box,
  Card,
  CardBody,
  CardHeader,
  CloseButton,
  Flex,
  HStack,
  Icon,
  IconButton,
  type StackProps,
  Text,
  Tooltip,
  VStack,
} from '@chakra-ui/react'
import { BiCollapseHorizontal, BiExpandHorizontal } from 'react-icons/bi'
import { VscGraph, VscGraphLine } from 'react-icons/vsc'

import { DataPoint } from '@/api/__generated__'
import { useGetSample } from '@/api/hooks/useGetMetrics/useGetSample.tsx'
import ClipboardCopyIconButton from '@/components/Chakra/ClipboardCopyIconButton.tsx'

import { ChartType } from '../../types.ts'
import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import LineChart from '../charts/LineChart.tsx'
import BarChart from '../charts/BarChart.tsx'

interface ChartContainerProps extends StackProps {
  chartType: ChartType
  metricName?: string
  onClose?: () => void
  canEdit?: boolean
}

const MAX_SERIES = 10

const ChartContainer: FC<ChartContainerProps> = ({ chartType, metricName, onClose, canEdit = true, ...props }) => {
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
      newSeries.length = Math.min(newSeries.length, MAX_SERIES)
      return newSeries
    })
  }, [data])

  if (!metricName) return null

  const metricInfo = extractMetricInfo(metricName)
  const { device, suffix, id } = metricInfo

  const Chart = chartType === ChartType.BAR_CHART ? BarChart : LineChart
  const ChartIcon = chartType === ChartType.BAR_CHART ? VscGraph : VscGraphLine
  return (
    <HStack alignItems={'flex-start'} gap={0} {...props} gridColumn={gridSpan ? '1 / span 2' : 'auto'}>
      <Card size={'sm'} w={'100%'}>
        <CardHeader>
          <Flex gap="4">
            <Flex flex="1" gap="4" alignItems="center" flexWrap="wrap">
              <Avatar icon={<ChartIcon fontSize={18} />} size={'sm'} />
              <Box>
                <Tooltip label={t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')} placement={'top'}>
                  <Text textOverflow={'ellipsis'}>{t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')}</Text>
                </Tooltip>
                <Text>{id}</Text>
              </Box>
            </Flex>
          </Flex>
        </CardHeader>
        <CardBody>
          <Chart
            data={series}
            metricName={metricName}
            aria-label={t(`metrics.${device}.${suffix}`).replaceAll('.', ' ')}
          />
        </CardBody>
      </Card>
      <VStack ml={1}>
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
