/**
 * Layout Options Drawer
 *
 * Slide-out drawer for fine-tuning layout algorithm parameters.
 * Shows different options based on selected algorithm using RJSF.
 */

import type { FC } from 'react'
import { useMemo } from 'react'
import type { IChangeEvent } from '@rjsf/core'
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

interface LayoutOptionsDrawerProps {
  isOpen: boolean
  onClose: () => void
  algorithmType: LayoutType | null
  options: LayoutOptions
}

const LayoutOptionsDrawer: FC<LayoutOptionsDrawerProps> = ({ isOpen, onClose, algorithmType, options }) => {
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
      console.log('📋 [Drawer] Submitting layout options:', data.formData)

      // Step 1: Update options in store
      setLayoutOptions(data.formData)

      // Step 2: Wait for persistence middleware to complete
      await new Promise((resolve) => setTimeout(resolve, 100))

      // Step 3: Apply layout - applyLayout now reads fresh state from store!
      console.log('🚀 [Drawer] Calling applyLayout (which will read fresh state)...')
      const result = await applyLayout()

      console.log('✅ [Drawer] Layout applied:', result)

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
      <DrawerContent aria-label="Layout Options Configuration">
        <DrawerCloseButton />
        <DrawerHeader>Layout Options</DrawerHeader>

        <DrawerBody>
          {algorithmType === LayoutType.MANUAL ? (
            <Text color="gray.600" _dark={{ color: 'gray.400' }} fontSize="sm">
              Manual layout has no configurable options. Nodes remain in their current positions.
            </Text>
          ) : !algorithmType ? (
            <Text color="gray.500" _dark={{ color: 'gray.400' }}>
              Select a layout algorithm to configure options
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
              <Button onClick={handleCancel}>Cancel</Button>
              <Button variant="primary" type="submit" form="layout-options-form">
                Apply Options
              </Button>
            </ButtonGroup>
          </DrawerFooter>
        )}
      </DrawerContent>
    </Drawer>
  )
}

export default LayoutOptionsDrawer
