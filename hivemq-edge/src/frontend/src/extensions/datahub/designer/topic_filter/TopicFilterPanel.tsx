import type { FC } from 'react'
import { useMemo } from 'react'
import type { Node } from 'reactflow'
import type { CustomValidator } from '@rjsf/utils'
import { Card, CardBody } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { PanelProps, TopicFilterData } from '@datahub/types.ts'
import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/designer/topic_filter/TopicFilterData.ts'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { validateDuplicates } from '@datahub/utils/rjsf.utils.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'

export const TopicFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { isLoading, data } = useGetAllDataPolicies()
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const listFilters = useMemo(() => {
    if (isLoading || !data) return undefined
    return data.items?.map((e) => e.matching.topicFilter)
  }, [data, isLoading])

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const customValidate: CustomValidator<TopicFilterData> = (formData, errors) => {
    const duplicates = validateDuplicates(formData?.['topics'] || [])
    const hasDuplicate = !!duplicates.size

    if (hasDuplicate) {
      for (const [key, value] of duplicates) {
        for (const index of value) {
          errors['topics']?.[index]?.addError(t('error.validation.topicFilter.duplicate', { filter: key }))
        }
      }
    }

    for (const [index, value] of (formData?.['topics'] || []).entries()) {
      if (listFilters?.includes(value))
        errors['topics']?.[index]?.addError(t('error.validation.topicFilter.alreadyMatching', { filter: value }))
    }

    return errors
  }

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          schema={MOCK_TOPIC_FILTER_SCHEMA.schema}
          uiSchema={MOCK_TOPIC_FILTER_SCHEMA.uiSchema}
          formData={formData}
          customValidate={customValidate}
          widgets={datahubRJSFWidgets}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
