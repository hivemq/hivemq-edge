import type { FC } from 'react'
import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import type { RJSFSchema, UiSchema, CustomValidator } from '@rjsf/utils'
import { parse } from 'protobufjs'
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

import type { PolicySchema } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import { MOCK_JSONSCHEMA_SCHEMA, MOCK_PROTOBUF_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.ts'
import { useCreateSchema } from '@datahub/api/hooks/DataHubSchemasService/useCreateSchema.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { encodeProtobufSchema, decodeProtobufSchema } from '@datahub/utils/protobuf.utils.ts'
import { ResourceWorkingVersion, SchemaType } from '@datahub/types.ts'
import type { SchemaData } from '@datahub/types.ts'
import { dataHubToastOption } from '@datahub/utils/toast.utils.ts'

export interface SchemaEditorProps {
  isOpen: boolean
  onClose: () => void
  schema?: PolicySchema // If provided, creates new version based on this schema; backend auto-increments from highest existing version
}

export const SchemaEditor: FC<SchemaEditorProps> = ({ isOpen, onClose, schema }) => {
  const { t } = useTranslation('datahub')
  const toast = useToast()
  const { data: allSchemas } = useGetAllSchemas()
  const createSchema = useCreateSchema()

  const [formData, setFormData] = useState<SchemaData | null>(null)
  const [hasErrors, setHasErrors] = useState(false)
  const [initialFormData, setInitialFormData] = useState<SchemaData | null>(null)
  const [isFormDirty, setIsFormDirty] = useState(false)

  // Initialize form data based on mode (create vs edit)
  useEffect(() => {
    if (!isOpen) return

    if (schema) {
      // Edit mode - load the provided schema to create new version
      const schemaType = enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON

      // Decode schema source based on type
      let schemaSource: string
      if (schemaType === SchemaType.PROTOBUF) {
        // PROTOBUF schemas are encoded as FileDescriptorSet - decode them
        try {
          schemaSource = decodeProtobufSchema(schema.schemaDefinition)
        } catch (e) {
          // Fallback to error message if decoding fails
          schemaSource = `// Error decoding PROTOBUF schema: ${e instanceof Error ? e.message : String(e)}`
        }
      } else {
        // JSON schemas are simple base64 encoded
        schemaSource = atob(schema.schemaDefinition)
      }

      const initialData: SchemaData = {
        name: schema.id,
        type: schemaType,
        version: ResourceWorkingVersion.MODIFIED,
        schemaSource,
        messageType: schema.arguments?.messageType, // Load messageType from arguments if present
      }
      setFormData(initialData)
      setInitialFormData(initialData)
      setIsFormDirty(false) // Reset dirty state
    } else {
      // Create mode - initialize empty form
      const initialData = {
        name: '',
        type: SchemaType.JSON,
        version: ResourceWorkingVersion.DRAFT,
        schemaSource: MOCK_JSONSCHEMA_SCHEMA,
      }
      setFormData(initialData)
      setInitialFormData(initialData)
      setIsFormDirty(false) // In create mode, form starts dirty once name is added
    }
  }, [isOpen, schema])

  const handleChange = useCallback(
    (changeEvent: IChangeEvent<SchemaData>, id?: string) => {
      const newData = changeEvent.formData
      setHasErrors(changeEvent.errors.length > 0)

      if (!newData) return

      let updatedData = newData

      // Handle type change - update schema source template
      if (id?.includes('type') && newData.type) {
        updatedData = {
          ...newData,
          version: newData.version || 1,
          schemaSource: newData.type === SchemaType.JSON ? MOCK_JSONSCHEMA_SCHEMA : MOCK_PROTOBUF_SCHEMA,
        }
      }

      setFormData(updatedData)

      // Check if editable content has changed (compare editable fields only)
      if (initialFormData) {
        const hasChanged =
          updatedData.name !== initialFormData.name ||
          updatedData.type !== initialFormData.type ||
          updatedData.schemaSource !== initialFormData.schemaSource
        setIsFormDirty(hasChanged)
      }
    },
    [initialFormData]
  )

  const customValidate: CustomValidator<SchemaData> = useCallback(
    (formData, errors) => {
      if (!formData) {
        return errors
      }

      const { type, schemaSource, name, messageType } = formData

      // Check for duplicate name when creating (not editing)
      if (!schema && name && allSchemas?.items) {
        const isDuplicate = allSchemas.items.some((existingSchema) => existingSchema.id === name)
        if (isDuplicate) {
          errors.name?.addError(t('error.validation.schema.duplicate', { name }))
        }
      }

      // Validate Protobuf schema
      if (type === SchemaType.PROTOBUF && schemaSource) {
        try {
          parse(schemaSource)
        } catch (e) {
          errors.schemaSource?.addError((e as SyntaxError).message)
        }

        // Validate messageType is required and exists in schema
        if (messageType) {
          // Optionally validate that messageType exists in the parsed schema
          try {
            const root = parse(schemaSource).root
            const messageExists = root.lookup(messageType)
            if (!messageExists) {
              errors.messageType?.addError(t('error.validation.schema.messageType.notFound', { messageType }))
            }
          } catch (e) {
            // Schema parsing already failed, skip this validation
          }
        }
      }

      // Validate JSON schema
      if (type === SchemaType.JSON && schemaSource) {
        try {
          JSON.parse(schemaSource)
        } catch (e) {
          errors.schemaSource?.addError((e as SyntaxError).message)
        }
      }

      return errors
    },
    [schema, allSchemas?.items, t]
  )

  const getUISchema = (formDataSchema: SchemaData | null): UiSchema => {
    const isEditing = !!schema
    return {
      'ui:order': ['name', 'version', 'type', 'schemaSource', 'messageType', '*'],
      name: {
        'ui:autofocus': !isEditing,
        'ui:placeholder': 'my-schema',
        'ui:readonly': isEditing, // Name cannot be changed when editing
      },
      type: {
        'ui:widget': 'select',
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:readonly': true, // Version is not editable - shows DRAFT or MODIFIED status
      },
      schemaSource: {
        'ui:widget':
          formDataSchema?.type === SchemaType.PROTOBUF ? 'application/octet-stream' : 'application/schema+json',
      },
      messageType: {
        'ui:widget': 'datahub:message-type', // Custom widget for protobuf message type selection
      },
    }
  }

  const handleSave = async () => {
    if (!formData || !formData.name) {
      // RJSF already validates required fields, this shouldn't happen
      return
    }

    try {
      let schemaDefinition: string

      // PROTOBUF schemas need special encoding (FileDescriptorSet)
      if (formData.type === SchemaType.PROTOBUF) {
        try {
          schemaDefinition = encodeProtobufSchema(formData.schemaSource || '')
        } catch (e) {
          throw new Error(t('error.validation.protobuf.encoding'))
        }
      } else {
        // JSON schemas - simple base64 encoding
        schemaDefinition = btoa(formData.schemaSource || '')
      }

      const payload: PolicySchema = {
        id: formData.name,
        type: formData.type,
        schemaDefinition,
      }

      // Add messageType to arguments for PROTOBUF schemas
      if (formData.type === SchemaType.PROTOBUF && formData.messageType) {
        payload.arguments = {
          messageType: formData.messageType,
        }
      }

      // Always create (schemas are versioned, so editing creates new version)
      await createSchema.mutateAsync(payload)
      toast({
        ...dataHubToastOption,
        title: t('resource.schema.save.success'),
        description: schema
          ? t('resource.schema.version.created', { name: formData.name })
          : t('resource.schema.create.description', { name: formData.name }),
        status: 'success',
      })

      onClose()
    } catch (error) {
      toast({
        ...dataHubToastOption,
        title: t('resource.schema.save.error'),
        description: error instanceof Error ? error.message : String(error),
        status: 'error',
      })
    }
  }

  const isLoading = createSchema.isPending

  return (
    <Drawer isOpen={isOpen} onClose={onClose} size="xl" placement="right">
      <DrawerOverlay />
      <DrawerContent data-testid="schema-editor-drawer">
        <DrawerCloseButton />
        <DrawerHeader>
          {schema ? t('resource.schema.editor.title.newVersion') : t('resource.schema.editor.title.create')}
        </DrawerHeader>

        <DrawerBody>
          {formData && (
            <ReactFlowSchemaForm
              id="schema-editor-form"
              schema={MOCK_SCHEMA_SCHEMA.schema as RJSFSchema}
              formData={formData}
              uiSchema={getUISchema(formData)}
              widgets={datahubRJSFWidgets}
              formContext={{ currentSchemaSource: formData.schemaSource }}
              customValidate={customValidate}
              onChange={handleChange}
              onError={(errors) => {
                console.log('XXXXXX schema', errors)
                setHasErrors(errors.length > 0)
              }}
              onSubmit={handleSave}
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
              type="submit"
              form="schema-editor-form"
              isLoading={isLoading}
              data-testid="save-schema-button"
              isDisabled={hasErrors || !isFormDirty}
            >
              {t('Listings.action.save')}
            </Button>
          </HStack>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}
