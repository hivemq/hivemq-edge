import { type FC } from 'react'
import { NodeToolbar, type NodeProps, type NodeToolbarProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, Icon } from '@chakra-ui/react'
import { LuPanelRightOpen } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

type ContextualToolbarProps = Pick<NodeProps, 'id'> & Pick<NodeToolbarProps, 'position'>

const ContextualToolbar: FC<ContextualToolbarProps> = ({ id }) => {
  const { t } = useTranslation()
  const { nodes } = useWorkspaceStore()

  const selectedNodes = nodes.filter(
    (node) => node.selected // && node.type === NodeTypes.ADAPTER_NODE && node.parentNode === undefined
  )

  const [mainNodes] = selectedNodes
  const isGroup = mainNodes?.id === id && mainNodes?.type === NodeTypes.CLUSTER_NODE
  const isInward =
    mainNodes?.id === id && [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE].includes(mainNodes?.type as NodeTypes)
  const isOutward = true // mainNodes?.id === id && isBidirectional(mainNodes?.data?.type)
  if (mainNodes?.id === id) console.log('XXXXX isGroup', isGroup, isInward, isOutward)

  return (
    <>
      <NodeToolbar
        isVisible={Boolean(mainNodes?.id === id)}
        position={Position.Right}
        role="toolbar"
        aria-label="sssssssss"
        style={{ display: 'flex', gap: '12px' }}
      >
        <ButtonGroup size="sm" variant="solid" colorScheme="blue" orientation="vertical">
          <IconButton
            size="sm"
            data-testid="node-group-toolbar-panel"
            icon={<Icon as={LuPanelRightOpen} boxSize={5} />}
            aria-label={t('workspace.grouping.command.overview')}
          />
        </ButtonGroup>
      </NodeToolbar>
    </>
  )
}

export default ContextualToolbar
