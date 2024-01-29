import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { MOCK_FUNCTION_SCHEMA } from '../../api/specs/'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { FunctionData, PanelProps } from '../../types.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@/extensions/datahub/components/helpers'

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
