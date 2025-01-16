import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonGroupProps, Divider } from '@chakra-ui/react'
import { LuCopy, LuDelete, LuFileEdit } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.tsx'

interface NodeToolbarProps extends ButtonGroupProps {
  onCopy?: (event: React.BaseSyntheticEvent) => void
  onDelete?: (event: React.BaseSyntheticEvent) => void
  onEdit?: (event: React.BaseSyntheticEvent) => void
  children?: React.ReactNode
  selectedNode: string
}

const NodeDatahubToolbar: FC<NodeToolbarProps> = ({ onCopy, onEdit, onDelete, children, ...props }) => {
  const { t } = useTranslation('datahub')
  const { isNodeEditable } = usePolicyGuards(props.selectedNode)

  return (
    <>
      {children && (
        <>
          {children}
          <Divider orientation="vertical" />
        </>
      )}
      <ToolbarButtonGroup orientation="horizontal" isAttached {...props}>
        <IconButton
          icon={<LuFileEdit />}
          data-testid="node-toolbar-edit"
          aria-label={t('Listings.action.edit')}
          onClick={onEdit}
        />
        <IconButton
          icon={<LuCopy />}
          data-testid="node-toolbar-copy"
          aria-label={t('Listings.action.copy')}
          onClick={onCopy}
          isDisabled={!isNodeEditable}
        />
      </ToolbarButtonGroup>
      <ToolbarButtonGroup orientation="horizontal" isAttached variant="outline" {...props}>
        <IconButton
          icon={<LuDelete />}
          data-testid="node-toolbar-delete"
          aria-label={t('Listings.action.delete')}
          colorScheme="red"
          onClick={onDelete}
          isDisabled={!isNodeEditable}
        />
      </ToolbarButtonGroup>
    </>
  )
}

export default NodeDatahubToolbar
