import { FC, useState } from 'react'
import {
  Avatar,
  Box,
  Card,
  CardBody,
  CardHeader,
  Flex,
  Heading,
  HStack,
  IconButton,
  Text,
  type CardProps,
} from '@chakra-ui/react'
import { BsFileEarmarkBarGraphFill } from 'react-icons/bs'
import { BiCollapseHorizontal, BiEdit } from 'react-icons/bi'

import LineChart from './LineChart.tsx'
import { getDefaultMetricsFor } from '@/modules/EdgeVisualisation/utils/nodes-utils.ts'
import { Node } from 'reactflow'
import { Adapter, Bridge } from '@/api/__generated__'

interface ChartContainerProps extends CardProps {
  node: Node<Bridge | Adapter>
}

const ChartContainer: FC<ChartContainerProps> = ({ node, ...props }) => {
  const metricsName = getDefaultMetricsFor(node)
  const [colSpan, setColSpan] = useState(1)

  return (
    <Card size={'sm'} {...props}>
      <CardHeader>
        <Flex gap="4">
          <Flex flex="1" gap="4" alignItems="center" flexWrap="wrap">
            <Avatar icon={<BsFileEarmarkBarGraphFill />} size={'sm'} />
            <Box>
              <Heading size="sm">
                Metric: {metricsName[0].replace('com.hivemq.edge.protocol-adapters.simulation.', '')}
              </Heading>
              <Text>This chart shows the ...</Text>
            </Box>
          </Flex>
          <HStack>
            <IconButton
              variant="outline"
              colorScheme="gray"
              aria-label="See menu"
              size={'sm'}
              icon={<BiCollapseHorizontal />}
            />
            <IconButton variant="outline" colorScheme="gray" aria-label="See menu" size={'sm'} icon={<BiEdit />} />
          </HStack>
        </Flex>
      </CardHeader>
      <CardBody>
        <LineChart metricName={metricsName[0]} />
      </CardBody>
    </Card>
  )
}

export default ChartContainer
