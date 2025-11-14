/**
 * Wizard Bridge Form
 *
 * Step 2: Configure bridge settings.
 * Reuses existing bridge schema and form infrastructure from BridgeEditorDrawer.
 */

import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import type { RJSFSchema } from '@rjsf/utils'

import { Button, DrawerHeader, DrawerBody, DrawerFooter, DrawerCloseButton, Heading, Flex } from '@chakra-ui/react'

import type { Bridge } from '@/api/__generated__'
import { bridgeSchema, bridgeUISchema } from '@/api/schemas'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { customUniqueBridgeValidate } from '@/modules/Bridges/utils/validation-utils'

interface WizardBridgeFormProps {
  onSubmit: (data: Bridge) => void
  onBack: () => void
}

/**
 * Step 2: Configure bridge
 * Reuses the same schema, uiSchema, and validation as BridgeEditorDrawer
 */
const WizardBridgeForm: FC<WizardBridgeFormProps> = ({ onSubmit, onBack }) => {
  const { t } = useTranslation()
  const { data: allBridges } = useListBridges()

  // Adapt the uiSchema for wizard context (always new bridge)
  const uiSchema = useMemo(() => {
    return {
      ...bridgeUISchema,
      id: {
        ...bridgeUISchema.id,
        'ui:disabled': false, // Always enabled in wizard
        'ui:options': { isNewBridge: true },
      },
    }
  }, [])

  const onValidate = (data: IChangeEvent<Bridge, RJSFSchema>) => {
    if (data.formData) {
      onSubmit(data.formData)
    }
  }

  return (
    <>
      <DrawerHeader borderBottomWidth="1px">
        <DrawerCloseButton onClick={onBack} />
        <Heading size="md">{t('workspace.wizard.bridge.configure')}</Heading>
      </DrawerHeader>

      <DrawerBody>
        <ChakraRJSForm
          id="wizard-bridge-form"
          schema={bridgeSchema}
          uiSchema={uiSchema}
          customValidate={customUniqueBridgeValidate(allBridges?.map((bridge) => bridge.id))}
          onSubmit={onValidate}
        />
      </DrawerBody>

      <DrawerFooter borderTopWidth="1px">
        <Flex width="100%" justifyContent="space-between">
          <Button variant="outline" onClick={onBack}>
            {t('workspace.wizard.bridge.back')}
          </Button>
          <Button variant="primary" type="submit" form="wizard-bridge-form">
            {t('workspace.wizard.bridge.submit')}
          </Button>
        </Flex>
      </DrawerFooter>
    </>
  )
}

export default WizardBridgeForm
