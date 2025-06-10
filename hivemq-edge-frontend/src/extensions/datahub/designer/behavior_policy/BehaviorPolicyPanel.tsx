import type { FC } from 'react'
import { useMemo } from 'react'
import type { Node } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'
import type { CustomValidator } from '@rjsf/utils'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { useGetAllBehaviorPolicies } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useGetAllBehaviorPolicies.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import type { BehaviorPolicyData, PanelProps, PublishQuotaArguments } from '@datahub/types.ts'
import { BehaviorPolicyType } from '@datahub/types.ts'

const UNLIMITED_PUBLISH = -1

export const BehaviorPolicyPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const { data: allPolicies } = useGetAllBehaviorPolicies({})
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<BehaviorPolicyData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const customValidate: CustomValidator<BehaviorPolicyData> = (formData, errors) => {
    if (!allPolicies) errors['id']?.addError(t('error.validation.behaviourPolicy.notLoading'))
    else {
      const isIdNotUnique = Boolean(allPolicies.items?.find((e) => e.id === formData?.id))
      if (isIdNotUnique) errors['id']?.addError(t('error.validation.behaviourPolicy.notUnique'))
    }
    if (formData?.model === BehaviorPolicyType.PUBLISH_QUOTA) {
      const { maxPublishes, minPublishes } = formData.arguments as PublishQuotaArguments
      if (maxPublishes !== UNLIMITED_PUBLISH && maxPublishes < minPublishes) {
        errors?.['arguments']?.['maxPublishes']?.addError(
          t('error.validation.behaviourPolicy.publishQuota.maxLessThanMin')
        )
        errors?.['arguments']?.['minPublishes']?.addError(
          t('error.validation.behaviourPolicy.publishQuota.minMoreThanMax')
        )
      }
    }
    return errors
  }

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          schema={MOCK_BEHAVIOR_POLICY_SCHEMA.schema}
          uiSchema={MOCK_BEHAVIOR_POLICY_SCHEMA.uiSchema}
          customValidate={customValidate}
          formData={data}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
