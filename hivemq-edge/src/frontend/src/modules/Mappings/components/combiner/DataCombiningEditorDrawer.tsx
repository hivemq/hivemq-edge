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
import type { DataCombining } from '@/api/__generated__'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import type { CombinerContext } from '../../types'

interface MappingDrawerProps<T> {
  schema: RJSFSchema
  uiSchema: UiSchema
  formContext?: CombinerContext
  formData: T
  onClose: () => void
  onSubmit: (newItem: T | undefined) => void
}

const DataCombiningEditorDrawer: FC<MappingDrawerProps<DataCombining>> = ({ onClose, onSubmit, ...props }) => {
  const { t } = useTranslation()

  return (
    <Drawer isOpen={true} placement="right" size="full" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('combiner.schema.mapping.panel.header')}>
        <DrawerCloseButton />
        <DrawerHeader>{t('combiner.schema.mapping.panel.header')}</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <ChakraRJSForm
                showNativeWidgets={false}
                id="combiner-mapping-form"
                schema={props.schema}
                uiSchema={props.uiSchema}
                formData={props.formData}
                formContext={props.formContext}
                onSubmit={(e) => {
                  onSubmit(e.formData)
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

export default DataCombiningEditorDrawer
