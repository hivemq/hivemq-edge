import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel, Input } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { getChakra } from '@/components/rjsf/utils/getChakra'
import type { ModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'
import { extractModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'

export const BehaviorModelReadOnlyDisplay = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const models = extractModelMetadata()
  const selectedModel = models.find((model: ModelMetadata) => model.id === props.value)

  const displayValue = selectedModel ? selectedModel.title : props.value || t('behaviorModel.notSelected')

  return (
    <FormControl mb={1} {...chakraProps} isReadOnly>
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <Input id={props.id} value={displayValue} isReadOnly isDisabled />
    </FormControl>
  )
}
