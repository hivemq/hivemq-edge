import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { BehaviorPolicyData, PanelProps } from '../../types.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'
import { MOCK_VBEHAVIOR_POLICY_SCHEMA } from '@/extensions/datahub/api/specs/BehaviorPolicyData.ts'

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
          schema={MOCK_VBEHAVIOR_POLICY_SCHEMA.schema}
          // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={data}
          onSubmit={onFormSubmit}
          onChange={() => console.log('changed')}
          onError={() => console.log('errors')}
        />
      </CardBody>
    </Card>
  )
}
