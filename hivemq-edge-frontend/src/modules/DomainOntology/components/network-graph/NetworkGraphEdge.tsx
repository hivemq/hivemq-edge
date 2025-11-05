import type { FC } from 'react'
import type { Edge, EdgeProps } from '@xyflow/react'
import { BaseEdge, EdgeLabelRenderer, getStraightPath } from '@xyflow/react'
import { Badge, Box } from '@chakra-ui/react'

import type { NetworkGraphEdgeData } from '@/modules/DomainOntology/hooks/useGetNetworkGraphData.ts'

const EDGE_COLORS = {
  NORTHBOUND: '#4299E1', // blue.400
  SOUTHBOUND: '#9F7AEA', // purple.400
  BRIDGE: '#ED8936', // orange.400
  COMBINER: '#ED64A6', // pink.400
  ASSET_MAPPER: '#38B2AC', // teal.400
}

const EDGE_LABELS = {
  NORTHBOUND: 'North',
  SOUTHBOUND: 'South',
  BRIDGE: 'Bridge',
  COMBINER: 'Combine',
  ASSET_MAPPER: 'Asset',
}

const NetworkGraphEdge: FC<EdgeProps<Edge<NetworkGraphEdgeData>>> = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  data,
  selected,
}) => {
  const [edgePath, labelX, labelY] = getStraightPath({
    sourceX,
    sourceY,
    targetX,
    targetY,
  })

  const transformationType = data?.transformationType || 'NORTHBOUND'
  const strokeColor = EDGE_COLORS[transformationType as keyof typeof EDGE_COLORS]
  const strokeWidth = selected ? 3 : 2
  const label = data?.label || EDGE_LABELS[transformationType as keyof typeof EDGE_LABELS]

  const getBadgeColor = () => {
    switch (transformationType) {
      case 'NORTHBOUND':
        return 'blue'
      case 'SOUTHBOUND':
        return 'purple'
      case 'BRIDGE':
        return 'orange'
      case 'COMBINER':
        return 'pink'
      case 'ASSET_MAPPER':
        return 'teal'
      default:
        return 'blue'
    }
  }

  return (
    <>
      <BaseEdge
        id={id as string}
        path={edgePath}
        style={{
          stroke: strokeColor,
          strokeWidth,
          opacity: selected ? 1 : 0.7,
        }}
      />
      <EdgeLabelRenderer>
        <Box
          position="absolute"
          transform={`translate(-50%, -50%) translate(${labelX}px,${labelY}px)`}
          pointerEvents="all"
          className="nodrag nopan"
        >
          <Badge colorScheme={getBadgeColor()} fontSize="xs" opacity={selected ? 1 : 0.8} boxShadow="sm">
            {label}
          </Badge>
        </Box>
      </EdgeLabelRenderer>
    </>
  )
}

export default NetworkGraphEdge
