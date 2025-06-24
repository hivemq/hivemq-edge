import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import type { CustomValidator } from '@rjsf/utils'
import type { Node } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import type { DataPolicyData, PanelProps } from '@datahub/types.ts'

export const DataPolicyPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { data: allPolicies, isLoading, isError, error, isSuccess } = useGetAllDataPolicies()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<DataPolicyData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  useEffect(() => {
    if (error) onFormError?.(error)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error])

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    if (isError) errors['id']?.addError(t('error.validation.dataPolicy.notLoading'))
    else if (allPolicies) {
      const isIdNotUnique = Boolean(allPolicies.items?.find((e) => e.id === formData?.id))
      if (isIdNotUnique) errors['id']?.addError(t('error.validation.dataPolicy.notUnique'))
    }
    return errors
  }

  return (
    <Card>
      {isLoading && <LoaderSpinner />}
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      {error && <ErrorMessage status="error" message={error.message} />}
      {isSuccess && (
        <CardBody>
          <ReactFlowSchemaForm
            isNodeEditable={isNodeEditable}
            schema={MOCK_DATA_POLICY_SCHEMA.schema}
            uiSchema={MOCK_DATA_POLICY_SCHEMA.uiSchema}
            formData={data}
            onSubmit={onFormSubmit}
            customValidate={customValidate}
          />
        </CardBody>
      )}
    </Card>
  )
}
