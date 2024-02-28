import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { ClientFilterData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/flow/client_filter/ClientFilterSchema.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'

export const ClientFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()

  const clients = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ClientFilterData> | undefined
    return adapterNode ? adapterNode.data.clients : null
  }, [selectedNode, nodes])

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_CLIENT_FILTER_SCHEMA.schema}
          uiSchema={MOCK_CLIENT_FILTER_SCHEMA.uiSchema}
          formData={{ clients: clients }}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
