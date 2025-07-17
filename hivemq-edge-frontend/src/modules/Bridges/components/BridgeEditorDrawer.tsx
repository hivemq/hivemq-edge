import type { FC, ReactNode } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  ButtonGroup,
  Card,
  CardBody,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  useDisclosure,
} from '@chakra-ui/react'

import { Capability } from '@/api/__generated__'
import { bridgeSchema, bridgeUISchema } from '@/api/schemas'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { customUniqueBridgeValidate } from '@/modules/Bridges/utils/validation-utils.ts'

interface BridgeEditorDrawerProps {
  isNew?: boolean
  children?: ReactNode
}

const BridgeEditorDrawer: FC<BridgeEditorDrawerProps> = ({ isNew }) => {
  const { t } = useTranslation()
  const { onClose } = useDisclosure()
  const hasPersistence = useGetCapability(Capability.id.MQTT_PERSISTENCE)
  const { data: allBridges } = useListBridges()

  const uiSchemaPersistence = useMemo(() => {
    const { id, ['ui:tabs']: uiTabs, persist } = bridgeUISchema
    const optional = {
      'ui:tabs': (uiTabs as { id: string }[]).filter((tab) => tab.id !== 'bridgePersist'),
      persist: {
        ...persist,
        'ui:widget': 'hidden',
      },
    }
    return {
      ...bridgeUISchema,
      id: {
        ...id,
        'ui:disabled': !isNew,
        'ui:options': { isNewBridge: isNew },
      },
      ...(!hasPersistence && optional),
    }
  }, [hasPersistence, isNew])

  return (
    <Drawer isOpen={true} placement="right" size="lg" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('bridge.drawer.label')}>
        <DrawerCloseButton />
        <DrawerHeader>{isNew ? t('bridge.drawer.title.create') : t('bridge.drawer.title.update')}</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <ChakraRJSForm
                showNativeWidgets={false}
                id="bridge-form"
                schema={bridgeSchema}
                uiSchema={uiSchemaPersistence}
                customValidate={customUniqueBridgeValidate(allBridges?.map((e) => e.id))}
                onSubmit={() => {
                  console.log('XXXX')
                }}
              />
            </CardBody>
          </Card>
        </DrawerBody>

        <DrawerFooter>
          {!isNew && (
            <ButtonGroup>
              <Button type="button" variant="danger" form="bridge-form" onClick={handleOnDelete} isDisabled>
                {t('bridge.action.delete')}
              </Button>
            </ButtonGroup>
          )}

          <ButtonGroup flexGrow={1} justifyContent="flex-end">
            <Button onClick={handleEditorOnClose}>{t('bridge.action.cancel')}</Button>
            <Button variant="primary" type="submit" form="bridge-form">
              {isNew ? t('bridge.action.create') : t('bridge.action.update')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default BridgeEditorDrawer
