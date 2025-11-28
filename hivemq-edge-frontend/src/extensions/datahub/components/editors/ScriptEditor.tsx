import type { FC } from 'react'
import { useCallback, useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import type { RJSFSchema, UiSchema } from '@rjsf/utils'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  HStack,
  useToast,
} from '@chakra-ui/react'

import type { CustomValidator } from '@rjsf/utils'

import { Script } from '@/api/__generated__'

import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.ts'
import { useCreateScript } from '@datahub/api/hooks/DataHubScriptsService/useCreateScript.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { ResourceWorkingVersion } from '@datahub/types.ts'
import type { FunctionData } from '@datahub/types.ts'
import { dataHubToastOption } from '@datahub/utils/toast.utils.ts'

const MOCK_JAVASCRIPT_SOURCE = `function transform(publish, context) {
    // Your transformation logic here
    return publish;
}`

export interface ScriptEditorProps {
  isOpen: boolean
  onClose: () => void
  script?: Script // If provided, creates new version based on this script; backend auto-increments from highest existing version
}

export const ScriptEditor: FC<ScriptEditorProps> = ({ isOpen, onClose, script }) => {
  const { t } = useTranslation('datahub')
  const toast = useToast()
  const { data: allScripts } = useGetAllScripts({})
  const createScript = useCreateScript()

  const [formData, setFormData] = useState<FunctionData | null>(null)
  const [hasValidationErrors, setHasValidationErrors] = useState(false)
  const [initialFormData, setInitialFormData] = useState<FunctionData | null>(null)
  const [isFormDirty, setIsFormDirty] = useState(false)

  // Initialize form data based on mode (create vs edit)
  useEffect(() => {
    if (!isOpen) return

    if (script) {
      // Edit mode - load the provided script to create new version
      const initialData = {
        name: script.id,
        type: 'Javascript' as const,
        version: ResourceWorkingVersion.MODIFIED,
        sourceCode: atob(script.source),
        description: script.description,
      }
      setFormData(initialData)
      setInitialFormData(initialData)
      setIsFormDirty(false) // Reset dirty state
    } else {
      // Create mode - initialize empty form
      const initialData = {
        name: '',
        type: 'Javascript' as const,
        version: ResourceWorkingVersion.DRAFT,
        sourceCode: MOCK_JAVASCRIPT_SOURCE,
        description: '',
      }
      setFormData(initialData)
      setInitialFormData(initialData)
      setIsFormDirty(false) // Form starts dirty once name is added
    }
  }, [isOpen, script])

  const handleChange = useCallback(
    (changeEvent: IChangeEvent<FunctionData>) => {
      const newData = changeEvent.formData
      if (!newData) return

      setFormData(newData)

      // Check if editable content has changed (compare editable fields only)
      if (initialFormData) {
        const hasChanged =
          newData.name !== initialFormData.name ||
          newData.sourceCode !== initialFormData.sourceCode ||
          newData.description !== initialFormData.description
        setIsFormDirty(hasChanged)
      }
    },
    [initialFormData]
  )

  const customValidate: CustomValidator<FunctionData> = useCallback(
    (formData, errors) => {
      if (!formData) {
        setHasValidationErrors(false)
        return errors
      }

      const { sourceCode, name } = formData
      let hasErrors = false

      // Check for duplicate name when creating (not editing)
      if (!script && name && allScripts?.items) {
        const isDuplicate = allScripts.items.some((existingScript) => existingScript.id === name)
        if (isDuplicate) {
          errors.name?.addError(t('error.validation.script.duplicate', { name }))
          hasErrors = true
        }
      }

      // Validate JavaScript syntax using browser's Function constructor
      if (sourceCode) {
        try {
          // Try to parse the JavaScript code - Function constructor will throw SyntaxError if invalid
          new Function(sourceCode)
        } catch (e) {
          errors.sourceCode?.addError((e as SyntaxError).message)
          hasErrors = true
        }
      }

      setHasValidationErrors(hasErrors)
      return errors
    },
    [script, allScripts?.items, t]
  )

  const getUISchema = (): UiSchema => {
    const isEditing = !!script
    return {
      'ui:order': ['name', 'version', 'description', 'sourceCode', '*'],
      name: {
        'ui:autofocus': !isEditing,
        'ui:placeholder': 'my-script',
        'ui:readonly': isEditing, // Name cannot be changed when editing
      },
      type: {
        'ui:widget': 'hidden', // Only Javascript type supported
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:readonly': true, // Version is not editable - shows DRAFT or MODIFIED status
      },
      description: {
        'ui:widget': 'textarea',
      },
      sourceCode: {
        'ui:widget': 'text/javascript',
      },
    }
  }

  const handleSave = async () => {
    if (!formData || !formData.name) {
      // RJSF already validates required fields, this shouldn't happen
      return
    }

    try {
      const payload: Script = {
        id: formData.name,
        functionType: Script.functionType.TRANSFORMATION,
        source: btoa(formData.sourceCode || ''),
        description: formData.description,
      }

      // Always create (scripts are versioned, so editing creates new version)
      await createScript.mutateAsync(payload)
      toast({
        ...dataHubToastOption,
        title: t('resource.script.save.success'),
        description: script
          ? t('resource.script.version.created', { name: formData.name })
          : t('resource.script.create.description', { name: formData.name }),
        status: 'success',
      })

      onClose()
    } catch (error) {
      toast({
        ...dataHubToastOption,
        title: t('resource.script.save.error'),
        description: error instanceof Error ? error.message : String(error),
        status: 'error',
      })
    }
  }

  const isLoading = createScript.isPending

  return (
    <Drawer isOpen={isOpen} onClose={onClose} size="xl" placement="right">
      <DrawerOverlay />
      <DrawerContent data-testid="script-editor-drawer">
        <DrawerCloseButton />
        <DrawerHeader>
          {script ? t('resource.script.editor.title.newVersion') : t('resource.script.editor.title.create')}
        </DrawerHeader>

        <DrawerBody>
          {formData && (
            <ReactFlowSchemaForm
              schema={MOCK_FUNCTION_SCHEMA.schema as RJSFSchema}
              formData={formData}
              uiSchema={getUISchema()}
              widgets={datahubRJSFWidgets}
              customValidate={customValidate}
              onChange={handleChange}
            />
          )}
        </DrawerBody>

        <DrawerFooter>
          <HStack spacing={3}>
            <Button variant="secondary" onClick={onClose} isDisabled={isLoading}>
              {t('Listings.action.cancel')}
            </Button>
            <Button
              variant="primary"
              onClick={handleSave}
              isLoading={isLoading}
              data-testid="save-script-button"
              isDisabled={hasValidationErrors || !isFormDirty}
            >
              {t('Listings.action.save')}
            </Button>
          </HStack>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}
