/**
 * Wizard Adapter Form
 *
 * Step 2: Configure adapter settings.
 * Uses standard drawer structure with proper header, body, and footer.
 */

import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import type { RJSFSchema } from '@rjsf/utils'

import { Box, Button, DrawerHeader, DrawerBody, DrawerFooter, DrawerCloseButton, Heading, Flex } from '@chakra-ui/react'

import type { Adapter, ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard'
import { NodeTypes } from '@/modules/Workspace/types'

import { customUniqueAdapterValidate } from '@/modules/ProtocolAdapters/utils/validation-utils'
import { getRequiredUiSchema } from '@/modules/ProtocolAdapters/utils/uiSchema.utils'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types'

interface WizardAdapterFormProps {
  protocolId: string | undefined
  onSubmit: (data: Adapter) => void
  onBack: () => void
}

/**
 * Step 2: Configure adapter
 * Shows the RJSF form for adapter configuration
 */
const WizardAdapterForm: FC<WizardAdapterFormProps> = ({ protocolId, onSubmit, onBack }) => {
  const { t } = useTranslation()
  const { data: protocolTypes } = useGetAdapterTypes()
  const { data: allAdapters } = useListProtocolAdapters()

  const { schema, uiSchema, name, logo, isDiscoverable } = useMemo(() => {
    const adapter: ProtocolAdapter | undefined = protocolTypes?.items?.find((e) => e.id === protocolId)
    const { configSchema, uiSchema, capabilities } = adapter || {}

    return {
      isDiscoverable: Boolean(capabilities?.includes('DISCOVER')),
      schema: configSchema,
      name: adapter?.name,
      logo: adapter?.logoUrl,
      uiSchema: getRequiredUiSchema(uiSchema, true), // isNewAdapter = true
    }
  }, [protocolTypes?.items, protocolId])

  const onValidate = (data: IChangeEvent<Adapter, RJSFSchema>) => {
    if (data.formData) {
      onSubmit(data.formData)
    }
  }

  const context: AdapterContext = {
    isEditAdapter: false,
    isDiscoverable: isDiscoverable,
    adapterType: protocolId,
    adapterId: undefined,
  }

  if (!schema) {
    return (
      <>
        <DrawerHeader borderBottomWidth="1px">
          <DrawerCloseButton onClick={onBack} />
        </DrawerHeader>
        <DrawerBody>
          <LoaderSpinner />
        </DrawerBody>
      </>
    )
  }

  return (
    <>
      <DrawerHeader borderBottomWidth="1px">
        <DrawerCloseButton onClick={onBack} />
        <Heading size="md">{t('workspace.wizard.adapter.configure')}</Heading>
        <Box mt={2}>
          <NodeNameCard name={name} type={NodeTypes.ADAPTER_NODE} icon={logo} />
        </Box>
      </DrawerHeader>

      <DrawerBody>
        <ChakraRJSForm
          id="wizard-adapter-form"
          schema={schema}
          uiSchema={uiSchema}
          formContext={context}
          onSubmit={onValidate}
          // @ts-ignore - Types need fixing
          customValidate={customUniqueAdapterValidate(schema, allAdapters)}
        >
          <Box display="none" />
        </ChakraRJSForm>
      </DrawerBody>

      <DrawerFooter borderTopWidth="1px">
        <Flex width="100%" justifyContent="space-between">
          <Button variant="outline" onClick={onBack}>
            {t('workspace.wizard.adapter.back')}
          </Button>
          <Button variant="primary" type="submit" form="wizard-adapter-form">
            {t('workspace.wizard.adapter.submit')}
          </Button>
        </Flex>
      </DrawerFooter>
    </>
  )
}

export default WizardAdapterForm
