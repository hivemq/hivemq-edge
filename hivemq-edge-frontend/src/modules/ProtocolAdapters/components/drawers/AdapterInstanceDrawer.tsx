import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'
import type { IChangeEvent } from '@rjsf/core'
import type { RJSFSchema } from '@rjsf/utils'

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
  Text,
} from '@chakra-ui/react'

import type { Adapter, ApiError, ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { customUniqueAdapterValidate } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { getRequiredUiSchema } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

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

  const { schema, uiSchema, name, logo, isDiscoverable } = useMemo(() => {
    const adapter: ProtocolAdapter | undefined = data?.items?.find((e) => e.id === adapterType)
    const { configSchema, uiSchema, capabilities } = adapter || {}

    return {
      isDiscoverable: Boolean(capabilities?.includes('DISCOVER')),
      schema: configSchema,
      name: adapter?.name,
      logo: adapter?.logoUrl,
      uiSchema: getRequiredUiSchema(uiSchema, isNewAdapter),
    }
  }, [data?.items, isNewAdapter, adapterType])

  const defaultValues = useMemo(() => {
    if (isNewAdapter || !adapterId) return undefined
    const { config } = allAdapters?.find((adapter) => adapter.id === adapterId) || {}

    return config
  }, [isNewAdapter, adapterId, allAdapters])

  const onValidate = (data: IChangeEvent<Adapter, RJSFSchema>) => {
    if (data.formData) onSubmit(data.formData)
  }

  const context: AdapterContext = {
    isEditAdapter: !isNewAdapter,
    isDiscoverable: isDiscoverable,
    adapterType: adapterType,
    adapterId: adapterId,
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
              <NodeNameCard name={name} type={NodeTypes.ADAPTER_NODE} icon={logo} />
            </DrawerHeader>
            <DrawerBody>
              {schema && (
                <ChakraRJSForm
                  id="adapter-instance-form"
                  schema={schema}
                  uiSchema={uiSchema}
                  formData={defaultValues}
                  formContext={context}
                  onSubmit={onValidate}
                  // TODO[NVL] Types need fixing
                  // @ts-ignore
                  customValidate={customUniqueAdapterValidate(schema, allAdapters)}
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
