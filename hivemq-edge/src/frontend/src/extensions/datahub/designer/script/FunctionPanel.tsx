import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { FunctionData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'

export const FunctionPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<FunctionData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_FUNCTION_SCHEMA.schema}
          uiSchema={MOCK_FUNCTION_SCHEMA.uiSchema}
          widgets={datahubRJSFWidgets}
          formData={data}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
