import type { FC } from 'react'
import {
  Badge,
  Box,
  Button,
  ButtonGroup,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Heading,
  HStack,
  IconButton,
  Text,
  VStack,
} from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'

import type { NetworkGraphNode } from '@/modules/DomainOntology/hooks/useGetNetworkGraphData.ts'

interface NetworkGraphDetailsPanelProps {
  node: NetworkGraphNode
  edges: Array<{ source: string; target: string; data?: { transformationType: string } }>
  onClose: () => void
  onNavigateToConfig?: (nodeId: string) => void
  onFilterByNode?: (nodeId: string) => void
}

const NODE_COLOR_SCHEMES = {
  TAG: 'blue',
  TOPIC: 'green',
  TOPIC_FILTER: 'purple',
} as const

const NetworkGraphDetailsPanel: FC<NetworkGraphDetailsPanelProps> = ({
  node,
  edges,
  onClose,
  onNavigateToConfig,
  onFilterByNode,
}) => {
  // Find connected nodes
  const connectedNodes = edges
    .filter((edge) => edge.source === node.id || edge.target === node.id)
    .map((edge) => ({
      id: edge.source === node.id ? edge.target : edge.source,
      direction: edge.source === node.id ? 'outgoing' : 'incoming',
      edgeType: edge.data?.transformationType || 'UNKNOWN',
    }))

  const outgoingCount = connectedNodes.filter((c) => c.direction === 'outgoing').length
  const incomingCount = connectedNodes.filter((c) => c.direction === 'incoming').length

  return (
    <Card data-testid="network-graph-details-panel" size="sm">
      {/* Header */}
      <CardHeader>
        <HStack justify="space-between">
          <Heading size="sm">Node Details</Heading>
          <IconButton
            aria-label="Close details panel"
            icon={<CloseIcon />}
            size="sm"
            variant="ghost"
            onClick={onClose}
          />
        </HStack>
      </CardHeader>

      {/* Body */}
      <CardBody>
        <VStack align="stretch" spacing={4}>
          {/* Node Identity */}
          <Box>
            <Badge colorScheme={NODE_COLOR_SCHEMES[node.data.type]} mb={2}>
              {node.data.type}
            </Badge>
            <Text fontSize="lg" fontWeight="bold">
              {node.data.label}
            </Text>
          </Box>

          {/* Connection Stats */}
          <Box>
            <Text fontSize="sm" color="gray.600" mb={1}>
              Connections
            </Text>
            <HStack spacing={4}>
              <Text fontSize="sm">{outgoingCount} outgoing</Text>
              <Text fontSize="sm">{incomingCount} incoming</Text>
            </HStack>
          </Box>

          {/* Connected Nodes List */}
          {connectedNodes.length > 0 && (
            <Box>
              <Text fontSize="sm" color="gray.600" mb={2}>
                Connected To
              </Text>
              <VStack align="stretch" spacing={2}>
                {connectedNodes.map((connection, index) => (
                  <HStack key={`${connection.id}-${index}`} spacing={2}>
                    <Badge size="sm" colorScheme={connection.direction === 'outgoing' ? 'green' : 'orange'}>
                      {connection.edgeType}
                    </Badge>
                    <Text fontSize="sm" isTruncated>
                      {connection.id.replace(/^(tag|topic|filter)-/, '')}
                    </Text>
                    <Text fontSize="xs" color="gray.500">
                      ({connection.direction})
                    </Text>
                  </HStack>
                ))}
              </VStack>
            </Box>
          )}
        </VStack>
      </CardBody>

      {/* Footer - Actions */}
      {(onNavigateToConfig || onFilterByNode) && (
        <CardFooter>
          <ButtonGroup size="sm" spacing={2}>
            {onNavigateToConfig && (
              <Button onClick={() => onNavigateToConfig(node.id)} variant="outline">
                View Configuration
              </Button>
            )}
            {onFilterByNode && (
              <Button onClick={() => onFilterByNode(node.id)} variant="outline">
                Filter Graph
              </Button>
            )}
          </ButtonGroup>
        </CardFooter>
      )}
    </Card>
  )
}

export default NetworkGraphDetailsPanel
