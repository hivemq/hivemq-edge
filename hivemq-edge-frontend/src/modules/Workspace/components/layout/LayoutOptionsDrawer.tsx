/**
 * Layout Options Drawer
 *
 * Slide-out drawer for fine-tuning layout algorithm parameters.
 * Shows different options based on selected algorithm using RJSF.
 */

import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import debug from 'debug'
import {
  Button,
  ButtonGroup,
  Card,
  CardBody,
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  DrawerFooter,
  Text,
} from '@chakra-ui/react'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import {
  dagreLayoutSchema,
  dagreLayoutUISchema,
  radialHubLayoutSchema,
  radialHubLayoutUISchema,
  colaForceLayoutSchema,
  colaForceLayoutUISchema,
  colaConstrainedLayoutSchema,
  colaConstrainedLayoutUISchema,
  manualLayoutSchema,
  manualLayoutUISchema,
} from '../../schemas'
import { LayoutType, type LayoutOptions } from '../../types/layout.ts'
import useWorkspaceStore from '../../hooks/useWorkspaceStore'
import { useLayoutEngine } from '../../hooks/useLayoutEngine'

const log = debug('workspace:layout:drawer')

interface LayoutOptionsDrawerProps {
  isOpen: boolean
  onClose: () => void
  algorithmType: LayoutType | null
  options: LayoutOptions
}

const LayoutOptionsDrawer: FC<LayoutOptionsDrawerProps> = ({ isOpen, onClose, algorithmType, options }) => {
  const { t } = useTranslation()
  // Get fresh hooks inside the component
  const { setLayoutOptions } = useWorkspaceStore()
  const { applyLayout } = useLayoutEngine()

  // Select appropriate schema and UI schema based on algorithm type
  const { schema, uiSchema } = useMemo(() => {
    switch (algorithmType) {
      case LayoutType.DAGRE_TB:
      case LayoutType.DAGRE_LR:
        return { schema: dagreLayoutSchema, uiSchema: dagreLayoutUISchema }
      case LayoutType.RADIAL_HUB:
        return { schema: radialHubLayoutSchema, uiSchema: radialHubLayoutUISchema }
      case LayoutType.COLA_FORCE:
        return { schema: colaForceLayoutSchema, uiSchema: colaForceLayoutUISchema }
      case LayoutType.COLA_CONSTRAINED:
        return { schema: colaConstrainedLayoutSchema, uiSchema: colaConstrainedLayoutUISchema }
      case LayoutType.MANUAL:
        return { schema: manualLayoutSchema, uiSchema: manualLayoutUISchema }
      default:
        return { schema: manualLayoutSchema, uiSchema: manualLayoutUISchema }
    }
  }, [algorithmType])

  const handleSubmit = async (data: IChangeEvent<LayoutOptions>) => {
    if (data.formData) {
      log('📋 [Drawer] Submitting layout options:', data.formData)

      // Step 1: Update options in store
      setLayoutOptions(data.formData)

      // Step 2: Wait for persistence middleware to complete
      await new Promise((resolve) => setTimeout(resolve, 100))

      // Step 3: Apply layout - applyLayout now reads fresh state from store!
      log('🚀 [Drawer] Calling applyLayout (which will read fresh state)...')
      const result = await applyLayout()

      log('✅ [Drawer] Layout applied:', result)

      // Step 4: Close the drawer
      onClose()
    }
  }

  const handleCancel = () => {
    onClose()
  }

  return (
    <Drawer isOpen={isOpen} placement="right" onClose={onClose} size="md" variant="hivemq" id="layout-options-drawer">
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.autoLayout.options.aria-label')}>
        <DrawerCloseButton />
        <DrawerHeader>{t('workspace.autoLayout.options.title')}</DrawerHeader>

        <DrawerBody>
          {algorithmType === LayoutType.MANUAL ? (
            <Text color="gray.600" _dark={{ color: 'gray.400' }} fontSize="sm">
              {t('workspace.autoLayout.options.manual.message')}
            </Text>
          ) : !algorithmType ? (
            <Text color="gray.500" _dark={{ color: 'gray.400' }}>
              {t('workspace.autoLayout.options.noAlgorithm.message')}
            </Text>
          ) : (
            <Card>
              <CardBody>
                <ChakraRJSForm
                  id="layout-options-form"
                  schema={schema}
                  uiSchema={uiSchema}
                  formData={options}
                  onSubmit={handleSubmit}
                  showNativeWidgets={false}
                />
              </CardBody>
            </Card>
          )}
        </DrawerBody>

        {algorithmType && algorithmType !== LayoutType.MANUAL && (
          <DrawerFooter>
            <ButtonGroup flexGrow={1} justifyContent="flex-end">
              <Button onClick={handleCancel}>{t('workspace.autoLayout.options.actions.cancel')}</Button>
              <Button variant="primary" type="submit" form="layout-options-form">
                {t('workspace.autoLayout.options.actions.apply')}
              </Button>
            </ButtonGroup>
          </DrawerFooter>
        )}
      </DrawerContent>
    </Drawer>
  )
}

export default LayoutOptionsDrawer
