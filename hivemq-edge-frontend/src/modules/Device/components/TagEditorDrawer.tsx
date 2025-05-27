import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { RJSFSchema, UiSchema } from '@rjsf/utils'
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
} from '@chakra-ui/react'

import type { DomainTag } from '@/api/__generated__'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import type { DeviceTagListContext } from '../types'

interface TagEditorDrawerProps<T> {
  schema: RJSFSchema
  uiSchema: UiSchema
  formContext?: DeviceTagListContext
  formData: T
  onClose: () => void
  onSubmit: (newItem: T | undefined) => void
}

const TagEditorDrawer: FC<TagEditorDrawerProps<DomainTag>> = (props) => {
  const { t } = useTranslation()
  return (
    <Drawer isOpen={true} placement="right" size="lg" onClose={props.onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('device.drawer.tagEditor.header')}>
        <DrawerCloseButton />
        <DrawerHeader>{t('device.drawer.tagEditor.header')}</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <ChakraRJSForm
                showNativeWidgets={false}
                id="tag--form"
                schema={props.schema}
                uiSchema={props.uiSchema}
                formData={props.formData}
                formContext={props.formContext}
                onSubmit={(e) => {
                  props.onSubmit(e.formData)
                }}
              />
            </CardBody>
          </Card>
        </DrawerBody>

        <DrawerFooter>
          <ButtonGroup>
            <Button onClick={props.onClose}>{t('action.cancel')}</Button>
            <Button variant="primary" type="submit" form="tag--form">
              {t('action.save')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default TagEditorDrawer
