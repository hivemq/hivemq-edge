import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import type { NodeAssetsType, NodeTypes } from '@/modules/Workspace/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import LicenseWarning from '@/modules/Pulse/components/activation/LicenseWarning.tsx'

interface PulsePropertyDrawerProps {
  nodeId: string
  selectedNode: NodeAssetsType
  isOpen: boolean
  onClose: () => void
  onEditEntity: () => void
}

const PulsePropertyDrawer: FC<PulsePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()

  return (
    <ExpandableDrawer
      header={t('topicFilter.manager.header')}
      subHeader={<NodeNameCard name={selectedNode.data.label} type={selectedNode.type as NodeTypes} />}
      isOpen={isOpen}
      onClose={onClose}
      closeOnOverlayClick={false}
    >
      <LicenseWarning />
    </ExpandableDrawer>
  )
}

export default PulsePropertyDrawer
