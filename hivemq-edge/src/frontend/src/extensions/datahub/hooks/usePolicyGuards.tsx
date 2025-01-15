import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { useMemo } from 'react'
import { Node } from 'reactflow'
import { DesignerStatus, TopicFilterData } from '@datahub/types.ts'
import { canDeleteNode } from '@datahub/utils/node.utils.ts'
import { Alert, AlertIcon, AlertTitle } from '@chakra-ui/react'

export const usePolicyGuards = (selectedNode: string) => {
  const { status } = useDataHubDraftStore()
  const { nodes } = useDataHubDraftStore()

  const adapterNode = useMemo(() => {
    return nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
  }, [selectedNode, nodes])

  const isPolicyEditable = useMemo(() => status !== DesignerStatus.LOADED, [status])

  const protectedNode = adapterNode && canDeleteNode(adapterNode, status)
  console.log('XXXXXX isPolicyEditable', isPolicyEditable, protectedNode)

  let guardAlert = null
  if (!isPolicyEditable) {
    guardAlert = (
      <Alert status="info" mb={5}>
        <AlertIcon />
        <AlertTitle>The policy is in read-only mode</AlertTitle>
        The element cannot be modified
      </Alert>
    )
  } else if (protectedNode && !protectedNode.delete) {
    guardAlert = (
      <Alert status="info" mb={5}>
        <AlertIcon />
        <AlertTitle>The element is protected</AlertTitle>
        {protectedNode?.error}
      </Alert>
    )
  }

  return { status, isNodeEditable: isPolicyEditable && protectedNode && protectedNode?.delete, guardAlert: guardAlert }
}
