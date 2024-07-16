import { FC } from 'react'
import { ButtonGroup, ButtonGroupProps } from '@chakra-ui/react'
import { LuCopy, LuDelete, LuFileEdit } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

interface NodeToolbarProps extends ButtonGroupProps {
  onCopy?: (event: React.BaseSyntheticEvent) => void
  onDelete?: (event: React.BaseSyntheticEvent) => void
  onEdit?: (event: React.BaseSyntheticEvent) => void
}

const NodeDatahubToolbar: FC<NodeToolbarProps> = (props) => {
  return (
    <ButtonGroup size="xs" variant="solid" colorScheme="gray" isAttached {...props}>
      <IconButton icon={<LuFileEdit />} aria-label="Edit" onClick={props.onEdit} />
      <IconButton icon={<LuCopy />} aria-label="Copy" onClick={props.onCopy} />
      <IconButton icon={<LuDelete />} aria-label="Delete" colorScheme="red" onClick={props.onDelete} />
    </ButtonGroup>
  )
}

export default NodeDatahubToolbar
