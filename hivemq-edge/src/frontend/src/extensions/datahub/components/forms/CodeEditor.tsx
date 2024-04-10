import { useMemo } from 'react'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { Editor } from '@monaco-editor/react'
import { FormControl, FormLabel, VStack } from '@chakra-ui/react'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const isReadOnly = useMemo(() => props.readonly || props.options.readonly, [props.readonly, props.options.readonly])

  return (
    <FormControl
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={isReadOnly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <VStack gap={3} alignItems="flex-start" id={props.id}>
        <Editor
          height="40vh"
          defaultLanguage={lng}
          defaultValue={props.value}
          value={props.value}
          onChange={(event) => props.onChange(event)}
          options={{ readOnly: isReadOnly }}
        />
      </VStack>
    </FormControl>
  )
}

export const JavascriptEditor = (props: WidgetProps) => CodeEditor('javascript', props)
export const JSONSchemaEditor = (props: WidgetProps) => CodeEditor('json', props)
export const ProtoSchemaEditor = (props: WidgetProps) => CodeEditor('proto', props)
