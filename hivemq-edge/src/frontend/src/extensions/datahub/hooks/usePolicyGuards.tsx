import { useTranslation } from 'react-i18next'
import { useMemo } from 'react'
import { Node } from 'reactflow'
import { Alert, AlertDescription, AlertIcon, AlertTitle } from '@chakra-ui/react'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DesignerStatus, TopicFilterData } from '@datahub/types.ts'
import { canDeleteNode } from '@datahub/utils/node.utils.ts'

export const usePolicyGuards = (selectedNode?: string) => {
  const { t } = useTranslation('datahub')
  const { status } = useDataHubDraftStore()
  const { nodes } = useDataHubDraftStore()

  const adapterNode = useMemo(() => {
    return nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
  }, [selectedNode, nodes])

  const isPolicyEditable = useMemo(() => status !== DesignerStatus.LOADED, [status])

  const protectedNode = adapterNode && canDeleteNode(adapterNode, status)

  let guardAlert = null
  if (!isPolicyEditable) {
    guardAlert = (
      <Alert status="info" mb={5}>
        <AlertIcon />
        <AlertTitle>{t('workspace.guards.readonly.title')}</AlertTitle>
        <AlertDescription>{t('workspace.guards.readonly.message')}</AlertDescription>
      </Alert>
    )
  } else if (protectedNode && !protectedNode.delete) {
    guardAlert = (
      <Alert status="info" mb={5}>
        <AlertIcon />
        <AlertTitle>{t('workspace.guards.protected.title')}</AlertTitle>
        <AlertDescription>{t('workspace.guards.protected.message')}</AlertDescription>
      </Alert>
    )
  }

  return {
    status,
    isPolicyEditable,
    isNodeEditable: isPolicyEditable && protectedNode && protectedNode?.delete,
    guardAlert: guardAlert,
  }
}
