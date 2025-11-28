import { type FC, useCallback, useMemo } from 'react'
import type { ActionMeta, OnChangeValue } from 'chakra-react-select'
import { CreatableSelect } from 'chakra-react-select'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { getChakra } from '@/components/rjsf/utils/getChakra'

import { extractProtobufMessageTypes } from '@datahub/utils/protobuf.utils.ts'
import type { SchemaData } from '@datahub/types.ts'

interface MessageTypeOption {
  label: string
  value: string
}

/**
 * Custom RJSF widget for selecting a message type from a protobuf schema
 * Extracts available message types from the schemaSource field and displays them in a dropdown
 * Allows manual creation if automatic extraction fails or user needs a custom type
 */
export const MessageTypeSelect: FC<WidgetProps> = (props) => {
  const { id, value, required, disabled, readonly, onChange, schema, formData, uiSchema, rawErrors } = props
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema })

  console.log('XXXXXX', schema, formData)
  // Extract message types from the schemaSource field in the root form data
  // Note: In RJSF, widget's formData prop contains the ENTIRE form's data, not just this field
  const options = useMemo<MessageTypeOption[]>(() => {
    const rootData = formData as SchemaData
    const schemaSource = rootData?.schemaSource

    if (!schemaSource) {
      return []
    }
    const messageTypes = extractProtobufMessageTypes(schemaSource)
    return messageTypes.map((messageType) => ({
      label: messageType,
      value: messageType,
    }))
  }, [formData])

  const selectedValue = useMemo<MessageTypeOption | null>(() => {
    if (!value) return null
    return { label: value, value }
  }, [value])

  const onSelectChange = useCallback<
    (newValue: OnChangeValue<MessageTypeOption, false>, actionMeta: ActionMeta<MessageTypeOption>) => void
  >(
    (newValue, actionMeta) => {
      if (actionMeta.action === 'select-option' && newValue) {
        onChange(newValue.value)
        return
      }
      if (actionMeta.action === 'create-option' && newValue) {
        onChange(newValue.value)
        return
      }
      if (actionMeta.action === 'clear') {
        onChange(undefined)
        return
      }
    },
    [onChange]
  )

  return (
    <FormControl
      {...chakraProps}
      isDisabled={disabled || readonly}
      isRequired={required}
      isReadOnly={readonly}
      isInvalid={rawErrors && rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={id} id={`${id}-label`}>
          {schema.title || 'Message Type'}
        </FormLabel>,
        false
      )}

      <CreatableSelect<MessageTypeOption, false>
        inputId={id}
        options={options}
        value={selectedValue}
        onChange={onSelectChange}
        isClearable={!required}
        isDisabled={disabled || readonly}
        placeholder={
          options.length === 0 ? t('resource.schema.messageType.noMessages') : t('resource.schema.messageType.select')
        }
        noOptionsMessage={() => t('resource.schema.messageType.noMessages')}
        formatCreateLabel={(inputValue) => t('resource.schema.messageType.create', { inputValue })}
      />
    </FormControl>
  )
}
