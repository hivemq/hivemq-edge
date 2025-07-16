import type { FC, ReactNode } from 'react'
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
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { bridgeSchema, bridgeUISchema } from '@/api/schemas'

interface BridgeEditorDrawerProps {
  isNew?: boolean
  children?: ReactNode
}

const BridgeEditorDrawer: FC<BridgeEditorDrawerProps> = () => {
  const { t } = useTranslation()
  const { onClose } = useDisclosure()

  return (
    <Drawer isOpen={true} placement="right" size="xl" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('combiner.schema.mapping.panel.header')}>
        <DrawerCloseButton />
        <DrawerHeader>{t('combiner.schema.mapping.panel.header')}</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <ChakraRJSForm
                showNativeWidgets={false}
                id="bridge-form"
                schema={bridgeSchema}
                uiSchema={bridgeUISchema}
                onSubmit={() => {
                  console.log('XXXX')
                }}
              />
            </CardBody>
          </Card>
        </DrawerBody>

        <DrawerFooter>
          <ButtonGroup>
            <Button onClick={onClose}>{t('combiner.schema.mapping.action.cancel')}</Button>
            <Button variant="primary" type="submit" form="combiner-mapping-form">
              {t('combiner.schema.mapping.action.save')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default BridgeEditorDrawer
