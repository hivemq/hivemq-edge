import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node } from 'reactflow'

import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import DomainOntologyManager from '@/modules/DomainOntology/DomainOntologyManager.tsx'

interface NodePropertyDrawerProps {
  nodeId: string
  selectedNode: Node
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const EdgePropertyDrawer: FC<NodePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()

  return (
    <ExpandableDrawer
      header={t('workspace.property.header', { context: selectedNode.type })}
      subHeader={<NodeNameCard type={NodeTypes.EDGE_NODE} name={t('branding.appName')} />}
      isOpen={isOpen}
      onClose={onClose}
    >
      <DomainOntologyManager />
    </ExpandableDrawer>
  )
}

export default EdgePropertyDrawer
