import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'

import { DataPolicyData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { CustomValidator } from '@rjsf/utils'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'
import { useTranslation } from 'react-i18next'

export const DataPolicyPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { data: allPolicies } = useGetAllDataPolicies()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<DataPolicyData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    if (!allPolicies) errors['id']?.addError(t('error.validation.dataPolicy.notLoading'))
    else {
      const isIdNotUnique = Boolean(allPolicies.items?.find((e) => e.id === formData?.id))
      if (isIdNotUnique) errors['id']?.addError(t('error.validation.dataPolicy.notUnique'))
    }
    return errors
  }

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_DATA_POLICY_SCHEMA.schema}
          uiSchema={MOCK_DATA_POLICY_SCHEMA.uiSchema}
          formData={data}
          onSubmit={onFormSubmit}
          customValidate={customValidate}
        />
      </CardBody>
    </Card>
  )
}
