import { labelValue, WidgetProps } from '@rjsf/utils'
import { Editor } from '@monaco-editor/react'
import { Button, FormControl, FormLabel, HStack, VStack } from '@chakra-ui/react'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { useTranslation } from 'react-i18next'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  return (
    <FormControl
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={props.readonly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(<FormLabel htmlFor={props.id}>{props.label}</FormLabel>, props.hideLabel || !props.label)}

      <VStack gap={3} alignItems="flex-start">
        <Editor
          height="40vh"
          // id={"schema-editor"}
          defaultLanguage={lng}
          defaultValue={props.value}
          onChange={(event) => props.onChange(event)}
        />
        <HStack>
          <Button variant="danger" isDisabled size="sm">
            {t('workspace.codeEditor.delete')}
          </Button>
          <Button isDisabled size="sm">
            {t('workspace.codeEditor.test')}
          </Button>
        </HStack>
      </VStack>
    </FormControl>
  )
}

export const JavascriptEditor = (props: WidgetProps) => CodeEditor('javascript', props)
export const JSONSchemaEditor = (props: WidgetProps) => CodeEditor('json', props)
export const ProtoSchemaEditor = (props: WidgetProps) => CodeEditor('proto', props)
