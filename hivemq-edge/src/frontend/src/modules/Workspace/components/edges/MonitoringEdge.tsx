import { FC, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath, Node } from 'reactflow'
import { Icon, useColorMode } from '@chakra-ui/react'
import { BiBarChartSquare } from 'react-icons/bi'
import { MdPolicy } from 'react-icons/md'

import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'

import { DataHubNodeType, PolicyType, TopicFilterData } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const MonitoringEdge: FC<EdgeProps> = ({
  source,
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  markerEnd,
  style,
}) => {
  const { t } = useTranslation()
  const { options } = useEdgeFlowContext()
  const navigate = useNavigate()
  const { colorMode } = useColorMode()
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  })
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)
  const { nodes: datahubNodes } = useDataHubDraftStore()
  const { nodes: workspaceNodes } = useWorkspaceStore()

  // TODO[NVL] Might be worth exporting as a custom hook if more usage
  const policyRoute = useMemo(() => {
    if (!hasDataHub) return undefined

    const sourceNode = workspaceNodes.find((node) => node.id === source)
    if (!sourceNode) return undefined

    const sourceFilters = datahubNodes.find((node: Node<TopicFilterData>) => {
      return node.type === DataHubNodeType.TOPIC_FILTER && node.data.adapter === sourceNode.data.id
    })
    if (!sourceFilters) return undefined

    return `${PolicyType.DATA_POLICY}/${sourceFilters.id}`
  }, [workspaceNodes, datahubNodes, source, hasDataHub])

  return (
    <>
      <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
      {options.showMonitoringOnEdge && (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,

              // everything inside EdgeLabelRenderer has no pointer events by default
              // if you have an interactive element, set pointer-events: all
              pointerEvents: 'all',
            }}
            className="nodrag nopan"
          >
            <IconButton
              aria-label={t('workspace.observability.aria-label', { device: source })}
              variant={colorMode === 'light' ? 'outline' : 'solid'}
              icon={<Icon as={BiBarChartSquare} boxSize={6} />}
              backgroundColor={colorMode === 'light' ? 'white' : 'gray.700'}
              color={style?.stroke}
              onClick={() => navigate(`/edge-flow/link/${id}`)}
              borderRadius={25}
            />
            {policyRoute && (
              <IconButton
                aria-label="Datahub Policy"
                variant={colorMode === 'light' ? 'outline' : 'solid'}
                icon={<Icon as={MdPolicy} boxSize={6} />}
                backgroundColor={colorMode === 'light' ? 'white' : 'gray.700'}
                color={style?.stroke}
                onClick={() => navigate(`/datahub/${policyRoute}`)}
                borderRadius={25}
              />
            )}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}

export default MonitoringEdge
