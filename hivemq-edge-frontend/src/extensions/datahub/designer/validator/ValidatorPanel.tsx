import type { FC } from 'react'
import { useEffect } from 'react'
import { useMemo } from 'react'
import type { Node } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import type { PanelProps, ValidatorData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

export const ValidatorPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ValidatorData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  useEffect(() => {
    if (!data) {
      onFormError?.(new Error(t('error.elementNotDefined.description', { nodeType: DataHubNodeType.VALIDATOR })))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data])

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      {!data && (
        <ErrorMessage
          type={t('error.elementNotDefined.title')}
          message={t('error.elementNotDefined.description', { nodeType: DataHubNodeType.VALIDATOR })}
        />
      )}
      {data && (
        <CardBody>
          <ReactFlowSchemaForm
            isNodeEditable={isNodeEditable}
            schema={MOCK_VALIDATOR_SCHEMA.schema}
            // uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
            formData={data}
            onSubmit={onFormSubmit}
          />
        </CardBody>
      )}
    </Card>
  )
}
