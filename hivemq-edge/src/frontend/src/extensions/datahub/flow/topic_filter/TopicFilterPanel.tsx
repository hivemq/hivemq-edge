import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { CustomValidator } from '@rjsf/utils'
import { Card, CardBody } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { PanelProps, TopicFilterData } from '@datahub/types.ts'
import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/flow/topic_filter/TopicFilterData.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { validateDuplicates } from '@datahub/utils/rjsf.utils.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/'
import { datahubRJSFWidgets } from '@datahub/flow/datahubRJSFWidgets.tsx'

export const TopicFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()

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
          errors['topics']?.[index]?.addError(t(`the topic ${key} is already defined`))
        }
      }
    }

    return errors
  }

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
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
