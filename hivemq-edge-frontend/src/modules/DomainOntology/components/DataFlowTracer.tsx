import type { FC } from 'react'
import { useState } from 'react'
import type { OptionProps, SingleValue } from 'chakra-react-select'
import { Select, chakraComponents } from 'chakra-react-select'
import {
  Badge,
  Box,
  Button,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  FormLabel,
  Heading,
  HStack,
  Radio,
  RadioGroup,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react'

import {
  useGetTraceDataFlow,
  type GraphNode,
  type TraceDirection,
  type TraceResult,
} from '@/modules/DomainOntology/hooks/useGetTraceDataFlow.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

const NODE_TYPE_COLORS = {
  TAG: 'blue',
  TOPIC: 'green',
  TOPIC_FILTER: 'purple',
} as const

const EDGE_TYPE_COLORS = {
  NORTHBOUND: 'blue',
  SOUTHBOUND: 'purple',
  BRIDGE: 'orange',
  COMBINER: 'pink',
  ASSET_MAPPER: 'teal',
} as const

const EDGE_TYPE_LABELS = {
  NORTHBOUND: 'Northbound Mapping',
  SOUTHBOUND: 'Southbound Mapping',
  BRIDGE: 'Bridge Subscription',
  COMBINER: 'Combiner',
  ASSET_MAPPER: 'Asset Mapper',
} as const

interface NodeOption {
  label: string
  value: string
  node: GraphNode
}

// Custom option renderer showing node type and label
const customComponents = {
  Option: (props: OptionProps<NodeOption>) => {
    const node = props.data.node
    return (
      <chakraComponents.Option {...props}>
        <HStack spacing={2}>
          <Badge colorScheme={NODE_TYPE_COLORS[node.type]} fontSize="xs">
            {node.type}
          </Badge>
          <Text>{node.label}</Text>
        </HStack>
      </chakraComponents.Option>
    )
  },
}

const DataFlowTracer: FC = () => {
  const { trace, getAvailableNodes, isLoading } = useGetTraceDataFlow()

  const [selectedNode, setSelectedNode] = useState<NodeOption | null>(null)
  const [direction, setDirection] = useState<TraceDirection>('DOWNSTREAM')
  const [traceResult, setTraceResult] = useState<TraceResult | null>(null)

  const availableNodes = getAvailableNodes()

  // Convert GraphNode[] to NodeOption[]
  const nodeOptions: NodeOption[] = availableNodes.map((node) => ({
    label: node.label,
    value: node.id,
    node,
  }))

  const handleTrace = () => {
    if (!selectedNode) return
    const result = trace(selectedNode.value, direction)
    setTraceResult(result)
  }

  const handleReset = () => {
    setTraceResult(null)
    setSelectedNode(null)
  }

  if (isLoading) return <LoaderSpinner />

  if (availableNodes.length === 0) {
    return <ErrorMessage type="No data available for tracing" />
  }

  return (
    <VStack align="stretch" spacing={4} data-testid="data-flow-tracer">
      {/* Configuration Card */}
      <Card>
        <CardHeader>
          <Heading size="sm">🔍 Trace Data Flow</Heading>
        </CardHeader>

        <CardBody>
          <VStack align="stretch" spacing={4}>
            {/* Node Selector */}
            <FormControl>
              <FormLabel>Start from:</FormLabel>
              <Select<NodeOption>
                placeholder="Select a node..."
                options={nodeOptions}
                value={selectedNode}
                onChange={(newValue: SingleValue<NodeOption>) => setSelectedNode(newValue)}
                components={customComponents}
                isClearable
                isSearchable
                selectedOptionStyle="check"
              />
            </FormControl>

            {/* Direction Selector */}
            <FormControl>
              <FormLabel>Direction:</FormLabel>
              <RadioGroup value={direction} onChange={(value) => setDirection(value as TraceDirection)}>
                <Stack direction="column" spacing={2}>
                  <Radio value="DOWNSTREAM">
                    <Text fontSize="sm">Downstream (where data goes)</Text>
                  </Radio>
                  <Radio value="UPSTREAM">
                    <Text fontSize="sm">Upstream (where data comes from)</Text>
                  </Radio>
                  <Radio value="BIDIRECTIONAL">
                    <Text fontSize="sm">Bidirectional (both upstream and downstream)</Text>
                  </Radio>
                </Stack>
              </RadioGroup>
            </FormControl>
          </VStack>
        </CardBody>

        <CardFooter>
          <HStack>
            <Button onClick={handleTrace} variant="primary" isDisabled={!selectedNode}>
              Trace Flow
            </Button>
            {traceResult && (
              <Button onClick={handleReset} variant="ghost">
                Reset
              </Button>
            )}
          </HStack>
        </CardFooter>
      </Card>

      {/* Results Card */}
      {traceResult && (
        <Card>
          <CardHeader>
            <HStack justify="space-between">
              <Heading size="sm">
                Trace Results ({traceResult.path.length} hop{traceResult.path.length === 1 ? '' : 's'})
              </Heading>
              {traceResult.hasCycles && <Badge colorScheme="orange">Cycles Detected</Badge>}
            </HStack>
          </CardHeader>

          <CardBody>
            <VStack align="stretch" spacing={4}>
              {traceResult.path.map((hop, index) => (
                <Box key={`${hop.node.id}-${index}`}>
                  {/* Node */}
                  <HStack spacing={3}>
                    <Badge colorScheme={NODE_TYPE_COLORS[hop.node.type]} fontSize="xs">
                      {hop.node.type}
                    </Badge>
                    <Text fontWeight="bold">{hop.node.label}</Text>
                  </HStack>

                  {/* Edge (transformation) */}
                  {hop.edge && (
                    <HStack pl={4} pt={2} pb={2} spacing={2}>
                      <Text fontSize="lg" color="gray.400">
                        ↓
                      </Text>
                      <Badge colorScheme={EDGE_TYPE_COLORS[hop.edge.type]} size="sm">
                        {EDGE_TYPE_LABELS[hop.edge.type]}
                      </Badge>
                      {hop.edge.metadata && (
                        <Text fontSize="xs" color="gray.600">
                          {Object.entries(hop.edge.metadata)
                            .filter(([key]) => key.includes('Name'))
                            .map(([, value]) => value)
                            .join(', ')}
                        </Text>
                      )}
                    </HStack>
                  )}
                </Box>
              ))}

              {/* Summary */}
              <Box pt={4} borderTopWidth={1} borderColor="gray.200">
                <Text fontSize="sm" color="gray.600">
                  <strong>Summary:</strong> Data flows from{' '}
                  <Badge colorScheme={NODE_TYPE_COLORS[traceResult.startNode.type]} mx={1}>
                    {traceResult.startNode.label}
                  </Badge>
                  {traceResult.endNodes.length > 0 && (
                    <>
                      to{' '}
                      {traceResult.endNodes.map((node, idx) => (
                        <Badge key={node.id} colorScheme={NODE_TYPE_COLORS[node.type]} mx={1}>
                          {node.label}
                          {idx < traceResult.endNodes.length - 1 && ','}
                        </Badge>
                      ))}
                    </>
                  )}
                  {traceResult.path.length === 1 && ' (no outgoing connections)'}
                </Text>
              </Box>
            </VStack>
          </CardBody>

          <CardFooter>
            <HStack spacing={2}>
              <Button size="sm" variant="outline" onClick={() => console.log('Highlight on graph')}>
                Highlight on Graph
              </Button>
              <Button size="sm" variant="outline" onClick={() => console.log('Export trace')}>
                Export Trace
              </Button>
            </HStack>
          </CardFooter>
        </Card>
      )}
    </VStack>
  )
}

export default DataFlowTracer
