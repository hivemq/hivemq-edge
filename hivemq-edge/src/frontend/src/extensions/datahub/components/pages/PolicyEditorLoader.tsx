import { FC } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { PolicyType } from '@datahub/types.ts'
import PolicyEditor from '@datahub/components/pages/PolicyEditor.tsx'

interface PolicyLoaderProps {
  policyId: string
}

export const DataPolicyLoader: FC<PolicyLoaderProps> = ({ policyId }) => {
  return <PolicyEditor />
}

export const BehaviorPolicyLoader: FC<PolicyLoaderProps> = ({ policyId }) => {
  return <PolicyEditor />
}

const PolicyEditorLoader: FC = () => {
  const { t } = useTranslation('datahub')
  const { policyType, policyId } = useParams()

  if (!policyType || !(policyType in PolicyType))
    return (
      <ErrorMessage
        type={t('error.notDefined.title') as string}
        message={t('error.notDefined.description') as string}
      />
    )

  if (policyId) {
    if (policyType === PolicyType.DATA_POLICY) return <DataPolicyLoader policyId={policyId} />
    if (policyType === PolicyType.BEHAVIOR_POLICY) return <BehaviorPolicyLoader policyId={policyId} />

    return (
      <ErrorMessage
        type={t('error.notDefined.title') as string}
        message={t('error.notDefined.description') as string}
      />
    )
  }

  return <PolicyEditor />
}

export default PolicyEditorLoader
