import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useHotkeys } from 'react-hotkeys-hook'
import { NodeRemoveChange, EdgeRemoveChange } from 'reactflow'
import { ListItem, Text, UnorderedList, useDisclosure, useToast, VStack } from '@chakra-ui/react'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DesignerStatus } from '@datahub/types.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'
import { canDeleteEdge, canDeleteNode } from '@datahub/utils/node.utils.ts'
import { DATAHUB_TOAST_ID, dataHubToastOption } from '@datahub/utils/toast.utils.ts'

const DeleteListener: FC = () => {
  const { t } = useTranslation('datahub')
  const { nodes, edges, status, onNodesChange, onEdgesChange, setStatus } = useDataHubDraftStore()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const toast = useToast()

  const selectedElements = useMemo(() => {
    const selectedNodes = nodes.filter((node) => node.selected)
    const selectedEdges = edges.filter((edge) => edge.selected)
    return { selectedNodes, selectedEdges }
  }, [edges, nodes])

  const SelectedElementsCount = useMemo(() => {
    const { selectedNodes, selectedEdges } = selectedElements
    return selectedNodes.length + selectedEdges.length
  }, [selectedElements])

  const deleteContext = useMemo(() => {
    const { selectedNodes, selectedEdges } = selectedElements
    if (selectedNodes.length === 0) return 'EDGE_ONLY'
    if (selectedEdges.length === 0) return 'MODE_ONLY'
    return 'BOTH'
  }, [selectedElements])

  useHotkeys([DATAHUB_HOTKEY.BACKSPACE, DATAHUB_HOTKEY.DELETE], () => {
    const { selectedNodes, selectedEdges } = selectedElements
    const canDeleteNodes = selectedNodes.map((node) => canDeleteNode(node, status))
    const canDeleteEdges = selectedEdges.map((edge) => canDeleteEdge(edge, nodes))

    const allElements = [...canDeleteNodes, ...canDeleteEdges]
    const canDeleteElements = allElements.every((element) => Boolean(element.delete))
    if (canDeleteElements) {
      return onOpen()
    }

    const allErrors = allElements.reduce<string[]>((acc, cur) => {
      if (cur.error && !acc.includes(cur.error)) {
        acc.push(cur.error)
      }
      return acc
    }, [])
    if (!toast.isActive(DATAHUB_TOAST_ID))
      toast({
        ...dataHubToastOption,
        id: DATAHUB_TOAST_ID,
        title: t('workspace.deletion.modal.header', { count: SelectedElementsCount }),
        status: 'error',
        description: (
          <VStack alignItems="flex-start">
            <Text>{t('workspace.deletion.guards.message', { count: SelectedElementsCount })}</Text>
            <UnorderedList>
              {allErrors.map((error, index) => (
                <ListItem key={`toto-${index}`}>{error}</ListItem>
              ))}
            </UnorderedList>
          </VStack>
        ),
      })
  })

  const handleConfirmOnSubmit = () => {
    const { selectedNodes, selectedEdges } = selectedElements
    onEdgesChange(selectedEdges.map<EdgeRemoveChange>((edge) => ({ id: edge.id, type: 'remove' })))
    onNodesChange(selectedNodes.map<NodeRemoveChange>((node) => ({ id: node.id, type: 'remove' })))
    setStatus(DesignerStatus.MODIFIED)
  }

  return (
    <ConfirmationDialog
      isOpen={isOpen}
      onClose={onClose}
      onSubmit={handleConfirmOnSubmit}
      message={t('workspace.deletion.modal.message', {
        context: deleteContext,
        count: SelectedElementsCount,
      })}
      header={t('workspace.deletion.modal.header', { count: SelectedElementsCount })}
    />
  )
}

export default DeleteListener
