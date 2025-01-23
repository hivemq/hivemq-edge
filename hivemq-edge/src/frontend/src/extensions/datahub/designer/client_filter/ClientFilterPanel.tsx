import type { FC } from 'react'
import { useMemo } from 'react'
import type { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import type { ClientFilterData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'

export const ClientFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const clients = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ClientFilterData> | undefined
    return adapterNode ? adapterNode.data.clients : null
  }, [selectedNode, nodes])

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          schema={MOCK_CLIENT_FILTER_SCHEMA.schema}
          uiSchema={MOCK_CLIENT_FILTER_SCHEMA.uiSchema}
          formData={{ clients: clients }}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
