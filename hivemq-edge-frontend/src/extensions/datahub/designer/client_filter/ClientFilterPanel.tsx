import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node } from '@xyflow/react'
import { Card, CardBody } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import type { ClientFilterData, PanelProps } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'

export const ClientFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const clients = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<ClientFilterData> | undefined
    return adapterNode ? adapterNode.data.clients : null
  }, [selectedNode, nodes])

  useEffect(() => {
    if (!clients) {
      onFormError?.(new Error(t('error.elementNotDefined.description', { nodeType: DataHubNodeType.CLIENT_FILTER })))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clients])

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      {!clients && (
        <ErrorMessage
          type={t('error.elementNotDefined.title')}
          message={t('error.elementNotDefined.description', { nodeType: DataHubNodeType.CLIENT_FILTER })}
        />
      )}
      {clients && (
        <CardBody>
          <ReactFlowSchemaForm
            isNodeEditable={isNodeEditable}
            schema={MOCK_CLIENT_FILTER_SCHEMA.schema}
            uiSchema={MOCK_CLIENT_FILTER_SCHEMA.uiSchema}
            formData={{ clients: clients }}
            onSubmit={onFormSubmit}
          />
        </CardBody>
      )}
    </Card>
  )
}
