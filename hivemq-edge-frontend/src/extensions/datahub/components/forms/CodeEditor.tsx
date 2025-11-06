import { useEffect, useMemo, useRef, useState } from 'react'
import { Editor, useMonaco } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { generateWidgets } from '@rjsf/chakra-ui'
import { FormControl, FormLabel, Text, useColorModeValue, useToken, VStack } from '@chakra-ui/react'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import monacoConfig from './monaco/monacoConfig'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const monaco = useMonaco()
  const [isLoaded, setIsLoaded] = useState(false)
  const [isConfigured, setIsConfigured] = useState(false)
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  const isUserEditingRef = useRef(false)

  // Debug logging
  useEffect(() => {
    console.log(`[Monaco ${lng}] Monaco instance:`, monaco ? 'LOADED' : 'NOT LOADED')
  }, [monaco, lng])

  const [editorBgLight, editorBgDark] = useToken('colors', ['white', 'gray.200'])
  const editorBackgroundColor = useColorModeValue(editorBgLight, editorBgDark)

  const { TextareaWidget } = generateWidgets()

  const handleEditorMount = (editor: editor.IStandaloneCodeEditor) => {
    editorRef.current = editor
  }

  const handleEditorChange = (value: string | undefined) => {
    // Mark that user is editing to prevent programmatic updates from interfering
    isUserEditingRef.current = true
    props.onChange(value)

    // Reset the flag after a short delay to allow programmatic updates again
    setTimeout(() => {
      isUserEditingRef.current = false
    }, 100)
  }

  useEffect(() => {
    if (editorRef.current && !isUserEditingRef.current && props.value !== editorRef.current.getValue()) {
      // Preserve cursor position during programmatic updates
      const position = editorRef.current.getPosition()
      editorRef.current.setValue(props.value || '')

      // Restore cursor position if it was valid
      if (position) {
        editorRef.current.setPosition(position)
      }
    }
  }, [props.value])

  const isReadOnly = useMemo(() => {
    return !!(props.readonly || props.options.readonly || props.disabled || props.options.disabled)
  }, [props.readonly, props.options.readonly, props.options.disabled, props.disabled])

  useEffect(() => {
    if (monaco && !isConfigured) {
      try {
        console.log('[Monaco] Configuring languages and themes...')
        // Configure all languages
        monacoConfig.configureLanguages(monaco)

        // Configure themes
        monacoConfig.configureThemes(monaco, {
          backgroundColor: editorBackgroundColor,
          isReadOnly,
        })

        setIsConfigured(true)
        setIsLoaded(true)
        console.log('[Monaco] Configuration complete!')
      } catch (error) {
        console.error('[Monaco] Failed to configure:', error)
        setIsLoaded(true) // Fall back to basic editor
      }
    }
  }, [monaco, isConfigured, editorBackgroundColor, isReadOnly])

  // Update theme when background color or readonly state changes
  useEffect(() => {
    if (monaco && isConfigured) {
      monacoConfig.configureThemes(monaco, {
        backgroundColor: editorBackgroundColor,
        isReadOnly,
      })
    }
  }, [monaco, isConfigured, editorBackgroundColor, isReadOnly])

  // Get enhanced editor options
  const editorOptions = useMemo(() => {
    if (!isConfigured) return { readOnly: isReadOnly }
    return monacoConfig.getEditorOptions(lng, isReadOnly)
  }, [lng, isReadOnly, isConfigured])

  if (!isLoaded) {
    const { options, ...rest } = props
    console.log(`[CodeEditor ${lng}] Monaco not loaded, using TextArea fallback`)

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
          theme={isReadOnly ? 'readOnlyTheme' : 'lightTheme'}
          onChange={handleEditorChange}
          onMount={(editor) => {
            handleEditorMount(editor)
            console.log(`[Monaco ${lng}] Monaco Editor mounted successfully`)
          }}
          options={editorOptions}
        />
      </VStack>
    </FormControl>
  )
}

export const JavascriptEditor = (props: WidgetProps) => CodeEditor('javascript', props)
export const JSONSchemaEditor = (props: WidgetProps) => CodeEditor('json', props)
export const ProtoSchemaEditor = (props: WidgetProps) => CodeEditor('proto', props)
