import { FC, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'
import { IChangeEvent } from '@rjsf/core'
import { IdSchema, RJSFSchema } from '@rjsf/utils'
import { RJSFValidationError } from '@rjsf/utils/src/types.ts'
import Form from '@rjsf/chakra-ui'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  HStack,
  Image,
  Text,
} from '@chakra-ui/react'
import { immutableJSONPatch, JSONPatchAdd, JSONPatchDocument } from 'immutable-json-patch'
import validator from '@rjsf/validator-ajv8'

import { Adapter, ApiError, ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { customFormatsValidator, customValidate } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import {
  adapterJSFFields,
  adapterJSFWidgets,
  getRequiredUiSchema,
} from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { getTopicPaths } from '@/modules/Workspace/utils/topics-utils.ts'

interface AdapterInstanceDrawerProps {
  adapterType?: string
  isNewAdapter?: boolean
  isOpen: boolean
  isSubmitting: boolean
  error?: ApiError | null
  onClose: () => void
  onSubmit: (data: Adapter) => void
  onDelete?: () => void
}

const FLAG_POST_VALIDATE = false

const AdapterInstanceDrawer: FC<AdapterInstanceDrawerProps> = ({
  adapterType,
  isNewAdapter = false,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()
  const { data: allAdapters } = useListProtocolAdapters()
  const { adapterId } = useParams()
  const [batchData, setBatchData] = useState<JSONPatchDocument | undefined>(undefined)

  const { schema, uiSchema, name, logo, isDiscoverable } = useMemo(() => {
    const adapter: ProtocolAdapter | undefined = data?.items?.find((e) => e.id === adapterType)
    const { configSchema, uiSchema, capabilities } = adapter || {}

    // TODO[NVL] This is still a hack; backend needs to provide identification of subscription properties
    const paths = getTopicPaths(configSchema || {})
    const subIndex = paths.shift()?.split('.').shift()
    const hideSubscriptionsKey =
      import.meta.env.VITE_FLAG_ADAPTER_SCHEMA_HIDE_SUBSCRIPTION === 'true' ? subIndex : undefined

    console.log('XXXXXX', subIndex, hideSubscriptionsKey)

    return {
      isDiscoverable: Boolean(capabilities?.includes('DISCOVER')),
      schema: configSchema,
      name: adapter?.name,
      logo: adapter?.logoUrl,
      uiSchema: getRequiredUiSchema(uiSchema, isNewAdapter, hideSubscriptionsKey),
    }
  }, [data?.items, isNewAdapter, adapterType])

  const defaultValues = useMemo(() => {
    if (isNewAdapter || !adapterId) return undefined
    const { config } = allAdapters?.find((adapter) => adapter.id === adapterId) || {}
    if (batchData) {
      return immutableJSONPatch(config, batchData)
    }
    return config
  }, [isNewAdapter, adapterId, allAdapters, batchData])

  const onValidate = (data: IChangeEvent<Adapter, RJSFSchema>) => {
    if (data.formData) onSubmit(data.formData)
  }

  const filterUnboundErrors = (errors: RJSFValidationError[]) => {
    // Hide the AJV8 validation error from the view. It has no other identifier so matching the text
    return errors.filter((error) => !error.stack.startsWith('no schema with key or ref'))
  }

  const context: AdapterContext = {
    isEditAdapter: !isNewAdapter,
    isDiscoverable: isDiscoverable,
    adapterType: adapterType,
    adapterId: adapterId,
    onBatchUpload: (idSchema: IdSchema<unknown>, batch) => {
      const path = idSchema.$id.replace('root_', '/').replaceAll('_', '/') + '/-'
      const operations: JSONPatchDocument = batch.map<JSONPatchAdd>((value) => ({ op: 'add', path, value }))

      if (schema && FLAG_POST_VALIDATE) {
        const updatedDocument = immutableJSONPatch(defaultValues, operations)
        const { $schema, ...rest } = schema
        const validate = validator.ajv.compile(rest)
        validate(updatedDocument)
      }

      setBatchData(operations)
    },
  }

  return (
    <Drawer variant="hivemq" closeOnOverlayClick={false} size="lg" isOpen={isOpen} placement="right" onClose={onClose}>
      <DrawerOverlay />
      <DrawerContent aria-label={t('protocolAdapter.drawer.label')}>
        {!schema && <LoaderSpinner />}
        {schema && (
          <>
            <DrawerCloseButton />
            <DrawerHeader id="adapter-instance-header" borderBottomWidth="1px">
              <Text>
                {isNewAdapter ? t('protocolAdapter.drawer.title.create') : t('protocolAdapter.drawer.title.update')}
              </Text>
              <HStack>
                <Image boxSize="30px" objectFit="scale-down" src={logo} aria-label={name} />
                <Text fontSize="md" fontWeight="500">
                  {name}
                </Text>
              </HStack>
            </DrawerHeader>
            <DrawerBody>
              {schema && (
                <Form
                  id="adapter-instance-form"
                  schema={schema}
                  uiSchema={uiSchema}
                  templates={{
                    ObjectFieldTemplate,
                    FieldTemplate,
                    BaseInputTemplate,
                    ArrayFieldTemplate,
                    ArrayFieldItemTemplate,
                  }}
                  liveValidate
                  onSubmit={onValidate}
                  validator={customFormatsValidator}
                  showErrorList="bottom"
                  onError={(errors) => console.log('XXXXXXX', errors)}
                  formData={defaultValues}
                  customValidate={customValidate(schema, allAdapters, t)}
                  transformErrors={filterUnboundErrors}
                  formContext={context}
                  widgets={adapterJSFWidgets}
                  fields={adapterJSFFields}
                />
              )}
            </DrawerBody>

            <DrawerFooter borderTopWidth="1px">
              <Flex flexGrow={1} justifyContent="flex-end">
                <Button variant="primary" type="submit" form="adapter-instance-form">
                  {isNewAdapter ? t('protocolAdapter.action.create') : t('protocolAdapter.action.update')}
                </Button>
              </Flex>
            </DrawerFooter>
          </>
        )}
      </DrawerContent>
    </Drawer>
  )
}

export default AdapterInstanceDrawer
