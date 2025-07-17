import type { FC, ReactNode } from 'react'
import { useEffect, useState, useMemo } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { IChangeEvent } from '@rjsf/core'
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

import type { Bridge } from '@/api/__generated__'
import { Capability } from '@/api/__generated__'
import { bridgeSchema, bridgeUISchema } from '@/api/schemas'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { customUniqueBridgeValidate } from '@/modules/Bridges/utils/validation-utils.ts'
import { useBridgeManager } from '@/modules/Bridges/hooks/useTopicFilterManager.tsx'
import { bridgeInitialState } from '@/modules/Bridges/utils/defaults.utils.ts'

interface BridgeEditorDrawerProps {
  isNew?: boolean
  children?: ReactNode
}

const BridgeEditorDrawer: FC<BridgeEditorDrawerProps> = ({ isNew }) => {
  const { t } = useTranslation()
  const { onClose, onOpen, isOpen } = useDisclosure()
  const hasPersistence = useGetCapability(Capability.id.MQTT_PERSISTENCE)
  const { data: allBridges } = useListBridges()
  const { bridgeId } = useParams()
  const navigate = useNavigate()
  const [formData, setFormData] = useState<Bridge>(bridgeInitialState)
  const { onCreate, onUpdate, onError } = useBridgeManager()

  useEffect(() => {
    if (!allBridges) return
    if (bridgeId) {
      const foundBridge = allBridges?.find((e) => e.id === bridgeId)
      if (foundBridge) {
        setFormData(foundBridge)
        onOpen()
      } else {
        onError(new Error(t('bridge.toast.view.noLongerExist', { id: bridgeId })), {
          id: 'bridge-open-noExist',
          title: t('bridge.toast.view.title'),
          description: t('bridge.toast.view.error'),
        })
        navigate('/mqtt-bridges', { replace: true })
      }
    } else {
      setFormData(bridgeInitialState)
      onOpen()
    }
  }, [allBridges, bridgeId, navigate, onError, onOpen, t])

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

  const handleEditorOnClose = () => {
    onClose()
    navigate('/mqtt-bridges')
  }

  const handleEditorOnSubmit = (data: IChangeEvent<Bridge>) => {
    const formData = data.formData
    if (!formData) {
      onError(new Error(t('bridge.toast.view.noLongerExist', { id: bridgeId })), {
        id: 'bridge-open-noExist',
        title: t('bridge.toast.view.title'),
        description: t('bridge.toast.view.error'),
      })
      return
    }

    if (bridgeId && !isNew) {
      onUpdate(bridgeId, formData).then(() => handleEditorOnClose())
    } else if (!bridgeId && isNew) {
      onCreate(formData).then(() => handleEditorOnClose())
    }
  }

  const handleOnDelete = () => {}

  return (
    <Drawer isOpen={isOpen} placement="right" size="lg" onClose={handleEditorOnClose} variant="hivemq">
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
                formData={formData}
                customValidate={customUniqueBridgeValidate(allBridges?.map((bridge) => bridge.id))}
                onSubmit={handleEditorOnSubmit}
                onChange={(e) => {
                  console.log('XXXX', e.formData)
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
