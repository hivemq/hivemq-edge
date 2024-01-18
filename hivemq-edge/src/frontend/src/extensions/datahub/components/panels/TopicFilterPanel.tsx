import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { CustomValidator } from '@rjsf/utils'
import { IChangeEvent } from '@rjsf/core'
import { Card, CardBody } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { MOCK_TOPIC_FILTER_SCHEMA } from '../../api/specs/TopicFilterData.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { PanelProps, TopicFilterData } from '../../types.ts'
import { ReactFlowSchemaForm } from '../helpers/ReactFlowSchemaForm.tsx'
import { validateDuplicates } from '@/extensions/datahub/utils/rjsf.utils.ts'

export const TopicFilterPanel: FC<PanelProps> = ({ selectedNode, onClose }) => {
  const { t } = useTranslation('datahub')
  const { nodes, onUpdateNodes } = useDataHubDraftStore()

  const topics = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TopicFilterData> | undefined
    return adapterNode ? adapterNode.data.topics : null
  }, [selectedNode, nodes])

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const { formData } = data
      onUpdateNodes(selectedNode, formData)
      onClose?.()
    },
    [selectedNode, onUpdateNodes, onClose]
  )

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
          liveValidate
          customValidate={customValidate}
          onChange={() => console.log('changed')}
          onSubmit={onFormSubmit}
          onError={() => console.log('errors')}
        />
      </CardBody>
    </Card>
  )
}
