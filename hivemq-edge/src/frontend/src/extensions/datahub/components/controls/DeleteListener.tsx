import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useHotkeys } from 'react-hotkeys-hook'
import { useDisclosure } from '@chakra-ui/react'
import { NodeRemoveChange, EdgeRemoveChange } from 'reactflow'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'
import { DesignerStatus } from '@datahub/types.ts'

const DeleteListener: FC = () => {
  const { t } = useTranslation('datahub')
  const { nodes, edges, onNodesChange, onEdgesChange, setStatus } = useDataHubDraftStore()
  const { isOpen, onOpen, onClose } = useDisclosure()

  const selectedElements = useMemo(() => {
    const selectedNodes = nodes.filter((node) => node.selected)
    const selectedEdges = edges.filter((edge) => edge.selected)
    return { selectedNodes, selectedEdges }
  }, [edges, nodes])

  useHotkeys([DATAHUB_HOTKEY.BACKSPACE, DATAHUB_HOTKEY.DELETE], () => {
    onOpen()
  })

  const handleConfirmOnSubmit = () => {
    const { selectedNodes, selectedEdges } = selectedElements
    onEdgesChange(selectedEdges.map<EdgeRemoveChange>((e) => ({ id: e.id, type: 'remove' })))
    onNodesChange(selectedNodes.map<NodeRemoveChange>((e) => ({ id: e.id, type: 'remove' })))
    setStatus(DesignerStatus.MODIFIED)
  }

  return (
    <ConfirmationDialog
      isOpen={isOpen}
      onClose={onClose}
      onSubmit={handleConfirmOnSubmit}
      message={t('workspace.deletion.modal.message')}
      header={t('workspace.deletion.modal.header')}
    />
  )
}

export default DeleteListener
