import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { OperationData, PanelProps } from '../../types.ts'
import { MOCK_OPERATION_SCHEMA } from '../../api/specs/OperationData.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm, datahubRJSFWidgets } from '../helpers'

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_OPERATION_SCHEMA.schema}
          uiSchema={MOCK_OPERATION_SCHEMA.uiSchema}
          formData={formData}
          widgets={datahubRJSFWidgets}
          noHtml5Validate={true}
          onSubmit={onFormSubmit}
          onChange={(e) => console.log('changed', e.formData)}
          onError={() => console.log('errors')}
        />
      </CardBody>
    </Card>
  )
}
