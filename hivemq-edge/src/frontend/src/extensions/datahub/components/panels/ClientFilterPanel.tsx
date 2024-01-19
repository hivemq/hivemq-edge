import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { IChangeEvent } from '@rjsf/core'
import { Card, CardBody } from '@chakra-ui/react'

import { MOCK_CLIENT_FILTER_SCHEMA } from '../../api/specs/ClientFilterData.ts'
import { ClientFilterData, PanelProps } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'

export const ClientFilterPanel: FC<PanelProps> = ({ selectedNode, onClose }) => {
  const { nodes, onUpdateNodes } = useDataHubDraftStore()

  const clients = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ClientFilterData> | undefined
    return adapterNode ? adapterNode.data.clients : null
  }, [selectedNode, nodes])

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const { formData } = data
      onUpdateNodes(selectedNode, formData)
      onClose?.()
    },
    [selectedNode, onUpdateNodes, onClose]
  )

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_CLIENT_FILTER_SCHEMA.schema}
          uiSchema={MOCK_CLIENT_FILTER_SCHEMA.uiSchema}
          formData={{ clients: clients }}
          onChange={() => console.log('changed')}
          onSubmit={onFormSubmit}
          onError={() => console.log('errors')}
        />
      </CardBody>
    </Card>
  )
}
