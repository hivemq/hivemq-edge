import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { PanelProps, TransitionData } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/flow/transition/TransitionData.ts'

export const TransitionPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TransitionData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_TRANSITION_SCHEMA.schema}
          // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={data}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
