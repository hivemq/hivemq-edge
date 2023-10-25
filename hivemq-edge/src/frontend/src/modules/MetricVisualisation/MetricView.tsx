import { useLocalStorage } from '@uidotdev/usehooks'
import { FC } from 'react'
import {
  Avatar,
  Box,
  Button,
  Card,
  CardBody,
  CardHeader,
  Flex,
  Grid,
  Heading,
  HStack,
  IconButton,
  Text,
} from '@chakra-ui/react'
import { MetricVisualisation } from '@/modules/MetricVisualisation/types.ts'
import { Node } from 'reactflow'
import { Adapter, Bridge } from '@/api/__generated__'
import { getDefaultMetricsFor } from '@/modules/EdgeVisualisation/utils/nodes-utils.ts'
import LineChart from '@/modules/MetricVisualisation/components/LineChart.tsx'
import { BsFileEarmarkBarGraphFill } from 'react-icons/bs'
import { BiCollapseHorizontal, BiEdit, BiExpandHorizontal } from 'react-icons/bi'
import ChartContainer from '@/modules/MetricVisualisation/components/ChartContainer.tsx'

interface MetricViewProps {
  id?: string
  node: Node<Bridge | Adapter>
}

const MetricView: FC<MetricViewProps> = ({ id, node }) => {
  const [, saveReport] = useLocalStorage<MetricVisualisation[]>(`reports-${id}`, [])

  return (
    <>
      <Grid gridTemplateColumns={'auto auto'}>
        <ChartContainer node={node} gridColumn={'1 / span 2'} />
        <ChartContainer node={node} />
      </Grid>
      <Button
        mt={3}
        size={'sm'}
        onClick={() => saveReport(() => [{ metricId: 'fddfffg', viewType: 'fff', label: 'ffgffgfg' }])}
      >
        Add a new chart
      </Button>
    </>
  )
}

export default MetricView
