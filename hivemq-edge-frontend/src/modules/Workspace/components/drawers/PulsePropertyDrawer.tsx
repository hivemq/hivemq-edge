import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'

import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import type { NodePulseType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import LicenseWarning from '@/modules/Pulse/components/activation/LicenseWarning.tsx'
import AssetsTable from '@/modules/Pulse/components/assets/AssetsTable.tsx'

interface PulsePropertyDrawerProps {
  nodeId: string
  selectedNode: NodePulseType
  isOpen: boolean
  onClose: () => void
}

const PulsePropertyDrawer: FC<PulsePropertyDrawerProps> = ({ isOpen, selectedNode, onClose }) => {
  const { t } = useTranslation()
  const { data: hasPulseCapability } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  return (
    <ExpandableDrawer
      header={t('workspace.property.header', { context: NodeTypes.PULSE_NODE })}
      subHeader={<NodeNameCard name={selectedNode.data.label} type={selectedNode.type as NodeTypes} />}
      isOpen={isOpen}
      onClose={onClose}
      closeOnOverlayClick={false}
    >
      {!hasPulseCapability && <LicenseWarning />}
      {hasPulseCapability && (
        <Card>
          <CardBody>
            <AssetsTable variant="summary" />
          </CardBody>
        </Card>
      )}
    </ExpandableDrawer>
  )
}

export default PulsePropertyDrawer
