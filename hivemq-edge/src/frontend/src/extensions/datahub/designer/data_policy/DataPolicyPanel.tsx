import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody, Spinner } from '@chakra-ui/react'
import { CustomValidator } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'

import { DataPolicyData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'

export const DataPolicyPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()
  const { t } = useTranslation('datahub')
  const { isLoading, error, data } = useGetAllDataPolicies()

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<DataPolicyData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const existingIds = useMemo(() => {
    if (!data) return []
    if (!data.items) return []

    return data.items.map((policy) => policy.id)
  }, [data])

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    if (error) errors['policyId']?.addError(error.message)

    if (existingIds.includes(formData?.policyId || ''))
      errors['policyId']?.addError(t(`the name ${formData?.policyId} is already defined`))

    return errors
  }

  if (isLoading) return <Spinner />

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_DATA_POLICY_SCHEMA.schema}
          uiSchema={MOCK_DATA_POLICY_SCHEMA.uiSchema}
          formData={formData}
          customValidate={customValidate}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
