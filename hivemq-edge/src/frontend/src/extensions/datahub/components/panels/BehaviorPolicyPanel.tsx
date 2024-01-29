import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '../../api/specs/'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { BehaviorPolicyData, PanelProps } from '../../types.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'

export const BehaviorPolicyPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<BehaviorPolicyData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_BEHAVIOR_POLICY_SCHEMA.schema}
          // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={data}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
