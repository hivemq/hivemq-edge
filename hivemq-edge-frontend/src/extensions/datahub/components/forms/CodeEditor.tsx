import { useEffect, useMemo, useState } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { Editor, useMonaco } from '@monaco-editor/react'
import { FormControl, FormLabel, Text, useColorModeValue, useToken, VStack } from '@chakra-ui/react'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import { generateWidgets } from '@rjsf/chakra-ui'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { useTranslation } from 'react-i18next'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const monaco = useMonaco()
  const [isLoaded, setIsLoaded] = useState(false)

  const [editorBgLight, editorBgDark] = useToken('colors', ['white', 'gray.200'])
  const editorBackgroundColor = useColorModeValue(editorBgLight, editorBgDark)

  const { TextareaWidget } = generateWidgets()

  useEffect(() => {
    if (monaco) {
      setIsLoaded(true)
    }
  }, [monaco])

  const isReadOnly = useMemo(() => {
    return props.readonly || props.options.readonly || props.disabled || props.options.disabled
  }, [props.readonly, props.options.readonly, props.options.disabled, props.disabled])

  useEffect(() => {
    if (monaco) {
      monaco.editor.defineTheme('lightTheme', {
        base: 'vs',
        inherit: true,
        rules: [],
        colors: {
          'editor.background': editorBackgroundColor,
        },
      })
    }

    if (monaco && isReadOnly) {
      monaco.editor.defineTheme('readOnlyTheme', {
        base: 'vs',
        inherit: false,
        rules: [
          { token: '', foreground: '#808080' }, // Gray for all tokens
        ],
        colors: {
          'editor.foreground': '#808080',
        },
      })
    }
  }, [monaco, isReadOnly, editorBackgroundColor])

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
          theme={isReadOnly ? 'readOnlyTheme' : 'lightTheme'}
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
