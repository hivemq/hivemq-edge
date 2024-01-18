import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { IChangeEvent } from '@rjsf/core'
import { Card, CardBody } from '@chakra-ui/react'

import { MOCK_VALIDATOR_SCHEMA } from '../../api/specs/DataPolicyValidator.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { PanelProps, ValidatorData } from '../../types.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'

export const ValidatorPanel: FC<PanelProps> = ({ selectedNode, onClose }) => {
  const { nodes, onUpdateNodes } = useDataHubDraftStore()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ValidatorData> | undefined
    return adapterNode ? adapterNode.data : null
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
          schema={MOCK_VALIDATOR_SCHEMA.schema}
          // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={data}
          onChange={() => console.log('changed')}
          onSubmit={onFormSubmit}
          onError={() => console.log('errors')}
        />
      </CardBody>
    </Card>
  )
}
