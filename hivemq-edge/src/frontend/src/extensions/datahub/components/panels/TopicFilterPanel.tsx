import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { CustomValidator } from '@rjsf/utils'
import { Card, CardBody } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { MOCK_TOPIC_FILTER_SCHEMA } from '../../api/specs/'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { PanelProps, TopicFilterData } from '../../types.ts'
import { validateDuplicates } from '../../utils/rjsf.utils.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'

export const TopicFilterPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()

  const topics = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
    return adapterNode ? adapterNode.data.topics : null
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
          formData={{ topics: topics }}
          customValidate={customValidate}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
