import { FC, useMemo } from 'react'
import { NodeToolbar, Position } from 'reactflow'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'
import { LuCopy, LuDelete, LuFileEdit } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

interface NodeToolbarProps extends ButtonGroupProps {
  isVisible: boolean
  onCopy?: (event: React.BaseSyntheticEvent) => void
  onDelete?: (event: React.BaseSyntheticEvent) => void
  onEdit?: (event: React.BaseSyntheticEvent) => void
}

const NodeDatahubToolbar: FC<NodeToolbarProps> = (props) => {
  const { nodes } = useDataHubDraftStore()

  const isSingleSelect = useMemo(() => {
    const selectedNodes = nodes.filter((node) => node.selected)
    return selectedNodes.length === 1
  }, [nodes])

  return (
    <NodeToolbar isVisible={props.isVisible && isSingleSelect} position={Position.Top} offset={8}>
      <ButtonGroup size="xs" variant="solid" colorScheme="gray" isAttached {...props}>
        <IconButton icon={<LuFileEdit />} aria-label="Edit" onClick={props.onEdit} />
        <IconButton icon={<LuCopy />} aria-label="Copy" onClick={props.onCopy} />
        <IconButton icon={<LuDelete />} aria-label="Delete" colorScheme="red" onClick={props.onDelete} />
      </ButtonGroup>
    </NodeToolbar>
  )
}

export default NodeDatahubToolbar
