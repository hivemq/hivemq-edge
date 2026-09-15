import { type FC, useCallback, useEffect, useMemo, useState } from 'react'
import type { ActionMeta, OnChangeValue } from 'chakra-react-select'
import { CreatableSelect } from 'chakra-react-select'
import type { WidgetProps } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { extractProtobufMessageTypes } from '@datahub/utils/protobuf.utils.ts'

interface MessageTypeOption {
  label: string
  value: string
}

interface SchemaFormContext {
  currentSchemaSource?: string
}

/**
 * Custom RJSF widget for selecting a message type from a protobuf schema
 * Uses formContext to access live schemaSource value (passed from SchemaEditor via formContext)
 * Extracts available message types from the schemaSource and displays them in a dropdown
 * Allows manual creation if automatic extraction fails or user needs a custom type
 */
export const MessageTypeSelect: FC<WidgetProps> = (props) => {
  const { id, value, onChange, required, disabled, readonly, schema, rawErrors, formContext } = props
  const { t } = useTranslation('datahub')
  const [options, setOptions] = useState<MessageTypeOption[]>([])
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  // Access schemaSource from formContext (passed by SchemaEditor)
  const context = formContext as SchemaFormContext | undefined
  const currentSchemaSource = context?.currentSchemaSource

  // Options are derived only while the menu is open: protobufjs `parse()` is real work and
  // `currentSchemaSource` is updated by SchemaEditor on every keystroke in the Monaco editor.
  //
  // Recomputing on every source change *while open*, rather than once in an onMenuOpen handler, is
  // what makes this correct: the editor change reaches formContext a render after Monaco emits it,
  // so a menu opened in that window used to capture an empty source and then stay stuck on "no
  // messages" until it was closed and reopened, because nothing recomputed in between.
  useEffect(() => {
    if (!isMenuOpen) {
      return
    }

    const schemaSource = currentSchemaSource || ''
    setOptions(
      schemaSource
        ? extractProtobufMessageTypes(schemaSource).map((messageType) => ({
            label: messageType,
            value: messageType,
          }))
        : []
    )
  }, [isMenuOpen, currentSchemaSource])

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

  const hasErrors = rawErrors && rawErrors.length > 0

  return (
    <FormControl
      data-testid={id}
      isDisabled={disabled || readonly}
      isRequired={required}
      isReadOnly={readonly}
      isInvalid={hasErrors}
    >
      <FormLabel htmlFor={id} id={`${id}-label`}>
        {schema.title || 'Message Type'}
      </FormLabel>

      <CreatableSelect<MessageTypeOption, false>
        id={`${id}-widget`}
        name={id}
        instanceId={id}
        inputId={id}
        options={options}
        value={selectedValue}
        onChange={onSelectChange}
        onMenuOpen={() => setIsMenuOpen(true)}
        onMenuClose={() => setIsMenuOpen(false)}
        isClearable={true}
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
