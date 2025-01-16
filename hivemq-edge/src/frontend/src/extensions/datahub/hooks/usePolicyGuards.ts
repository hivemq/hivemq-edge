import { useTranslation } from 'react-i18next'
import { useMemo } from 'react'
import { Node } from 'reactflow'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DesignerStatus, TopicFilterData } from '@datahub/types.ts'
import { canDeleteNode } from '@datahub/utils/node.utils.ts'

export interface GuardAlertProps {
  title: string
  description: string
}

export const usePolicyGuards = (selectedNode?: string) => {
  const { t } = useTranslation('datahub')
  const { status, nodes } = useDataHubDraftStore()

  const adapterNode = useMemo(() => {
    return nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
  }, [selectedNode, nodes])

  const isPolicyEditable = useMemo(() => status !== DesignerStatus.LOADED, [status])

  const protectedNode = adapterNode && canDeleteNode(adapterNode, status)

  let guardAlert: GuardAlertProps | undefined = undefined
  if (!isPolicyEditable) {
    guardAlert = {
      title: t('workspace.guards.readonly.title'),
      description: t('workspace.guards.readonly.message'),
    }
  } else if (protectedNode && !protectedNode.delete) {
    guardAlert = {
      title: t('workspace.guards.protected.title'),
      description: t('workspace.guards.protected.message'),
    }
  }

  return {
    status,
    isPolicyEditable,
    isNodeEditable: isPolicyEditable && protectedNode && protectedNode?.delete,
    guardAlert: guardAlert,
  }
}
