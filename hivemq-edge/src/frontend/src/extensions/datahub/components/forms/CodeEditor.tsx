import { useEffect, useMemo, useState } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { Editor, useMonaco } from '@monaco-editor/react'
import { FormControl, FormLabel, Text, VStack } from '@chakra-ui/react'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { generateWidgets } from '@rjsf/chakra-ui'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { useTranslation } from 'react-i18next'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const monaco = useMonaco()
  const [isLoaded, setIsLoaded] = useState(false)

  const { TextareaWidget } = generateWidgets()

  useEffect(() => {
    if (monaco) {
      setIsLoaded(true)
    }
  }, [monaco])

  const isReadOnly = useMemo(() => props.readonly || props.options.readonly, [props.readonly, props.options.readonly])

  if (!isLoaded) {
    const { options, ...rest } = props

    return (
      <>
        <TextareaWidget {...rest} options={{ ...options, rows: 6 }} />
        <Text fontSize="sm">{t('workspace.codeEditor.loadingError')}</Text>
      </>
    )
  }
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
          loading={<LoaderSpinner />}
          height="40vh"
          defaultLanguage={lng}
          defaultValue={props.value}
          value={props.value}
          onChange={(event) => props.onChange(event)}
          options={{ readOnly: isReadOnly }}
        />
        )
      </VStack>
    </FormControl>
  )
}

export const JavascriptEditor = (props: WidgetProps) => CodeEditor('javascript', props)
export const JSONSchemaEditor = (props: WidgetProps) => CodeEditor('json', props)
export const ProtoSchemaEditor = (props: WidgetProps) => CodeEditor('proto', props)
