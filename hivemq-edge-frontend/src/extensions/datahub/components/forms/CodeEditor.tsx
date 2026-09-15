import { useEffect, useMemo, useRef, useState } from 'react'
import debug from 'debug'
import { Editor, useMonaco } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'
import type { MonacoInstance } from './monaco/types'
// Side effect: binds the loader to the installed Monaco instead of the jsdelivr CDN
import './monaco/setupMonacoLoader.ts'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { generateWidgets } from '@rjsf/chakra-ui'
import { FormControl, FormLabel, Text, useColorModeValue, useToken, VStack } from '@chakra-ui/react'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import monacoConfig from './monaco/monacoConfig'
import { addDataHubActionsToEditor } from './monaco/languages/datahub-commands'

const debugLogger = debug('DataHub:monaco')

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const monaco = useMonaco()
  const [isLoaded, setIsLoaded] = useState(false)
  const [isConfigured, setIsConfigured] = useState(false)
  const [isEditorMounted, setIsEditorMounted] = useState(false)
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  const isUserEditingRef = useRef(false)

  // Debug logging
  useEffect(() => {
    debugLogger(`[${lng}] Monaco instance:`, monaco ? 'LOADED' : 'NOT LOADED')
  }, [monaco, lng])

  const [editorBgLight, editorBgDark] = useToken('colors', ['white', 'gray.200'])
  const editorBackgroundColor = useColorModeValue(editorBgLight, editorBgDark)

  const { TextareaWidget } = generateWidgets()

  /**
   * Monaco Editor Keyboard Shortcuts:
   *
   * Code Completion:
   * - Ctrl+Space (Windows/Linux) or Cmd+Space (Mac) - Manually trigger suggestions
   * - Ctrl+I - Alternative trigger for suggestions
   * - Tab - Accept selected suggestion
   * - Enter - Accept selected suggestion
   * - Esc - Dismiss suggestion widget
   *
   * Other:
   * - Ctrl+/ - Toggle line comment
   * - Ctrl+F - Find
   * - Ctrl+H - Find and Replace
   * - Alt+Up/Down - Move line up/down
   */
  const handleEditorMount = (editor: editor.IStandaloneCodeEditor, monacoInstance: MonacoInstance) => {
    editorRef.current = editor

    // Publishes "this editor now reports its changes". @monaco-editor/react creates the editor, then
    // calls onMount and subscribes to onDidChangeModelContent from two effects of the same commit, in
    // that order - so a content change made after this state update has rendered is guaranteed to
    // reach onChange, while one made between create() and that subscription is silently dropped. The
    // editor is in the DOM, visible and listed by monaco.editor.getEditors() for the whole of that
    // window, so nothing else distinguishes it; see MonacoEditorPOM.setValue.
    setIsEditorMounted(true)

    // Add DataHub custom actions to JavaScript editors. The instance comes from the mount callback
    // rather than the `useMonaco()` state: that state is set from an effect, so it can still be null
    // when the editor mounts, and the actions would then silently never be registered.
    if (lng === 'javascript') {
      try {
        addDataHubActionsToEditor(editor, monacoInstance)
        debugLogger('[javascript] DataHub actions added to editor')
      } catch (error) {
        debugLogger('[javascript] Failed to add DataHub actions:', error)
      }
    }

    // Fix: Monaco's keyboard handler doesn't process SPACE correctly in some contexts
    // Manually handle SPACE key insertion
    const editorDom = editor.getDomNode()
    if (editorDom) {
      editorDom.addEventListener(
        'keydown',
        (e) => {
          if (e.key === ' ') {
            // Monaco's keyboard handler is broken for SPACE - manually insert it
            const position = editor.getPosition()
            const model = editor.getModel()
            if (position && model) {
              e.preventDefault()
              e.stopPropagation()

              // Manually insert space
              editor.executeEdits('keyboard', [
                {
                  range: {
                    startLineNumber: position.lineNumber,
                    startColumn: position.column,
                    endLineNumber: position.lineNumber,
                    endColumn: position.column,
                  },
                  text: ' ',
                },
              ])

              // Move cursor after the space
              editor.setPosition({
                lineNumber: position.lineNumber,
                column: position.column + 1,
              })
            }
          }
        },
        { capture: true }
      )
    }
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
        debugLogger(`[${lng}] Configuring languages and themes...`)
        // Configure all languages
        monacoConfig.configureLanguages(monaco)

        // Configure themes
        monacoConfig.configureThemes(monaco, {
          backgroundColor: editorBackgroundColor,
          isReadOnly,
        })

        setIsConfigured(true)
        setIsLoaded(true)
        debugLogger(`[${lng}] Configuration complete!`)
      } catch (error) {
        debugLogger(`[${lng}] Failed to configure:`, error)
        setIsLoaded(true) // Fall back to basic editor
      }
    }
  }, [monaco, isConfigured, editorBackgroundColor, isReadOnly, lng])

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
    debugLogger(`[${lng}] Monaco not loaded, using TextArea fallback`)

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

      <VStack gap={3} alignItems="flex-start" id={props.id} data-editor-ready={isEditorMounted || undefined}>
        <Editor
          loading={<LoaderSpinner />}
          height="40vh"
          defaultLanguage={lng}
          defaultValue={props.value}
          theme={isReadOnly ? 'readOnlyTheme' : 'lightTheme'}
          onChange={handleEditorChange}
          onMount={(editor, monacoInstance) => {
            handleEditorMount(editor, monacoInstance)
            debugLogger(`[${lng}] Monaco Editor mounted successfully`)
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
