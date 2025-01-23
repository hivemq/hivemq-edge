import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { ButtonGroupProps } from '@chakra-ui/react'
import { Divider, Icon } from '@chakra-ui/react'
import { LuCopy, LuDelete, LuFileCog } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

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
          icon={<Icon as={LuFileCog} boxSize="20px" />}
          data-testid="node-toolbar-config"
          aria-label={t('Listings.action.config')}
          onClick={onEdit}
        />
        <IconButton
          icon={<Icon as={LuCopy} boxSize="20px" />}
          data-testid="node-toolbar-copy"
          aria-label={t('Listings.action.copy')}
          onClick={onCopy}
          isDisabled={!isNodeEditable}
        />
      </ToolbarButtonGroup>
      <ToolbarButtonGroup orientation="horizontal" isAttached variant="outline" {...props}>
        <IconButton
          icon={<Icon as={LuDelete} boxSize="20px" />}
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
