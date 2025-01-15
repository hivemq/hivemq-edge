import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { DataHubNodeType, PanelProps, ValidatorData } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.tsx'

export const ValidatorPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ValidatorData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  if (!data)
    return (
      <ErrorMessage
        type={t('error.elementNotDefined.title')}
        message={t('error.elementNotDefined.description', { nodeType: DataHubNodeType.VALIDATOR })}
      />
    )

  return (
    <Card>
      {guardAlert && guardAlert}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          schema={MOCK_VALIDATOR_SCHEMA.schema}
          // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={data}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
