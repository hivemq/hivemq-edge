import { FC } from 'react'
import {
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  Button,
  Card,
  CardBody,
  ButtonGroup,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { SouthboundMapping, JsonNode } from '@/api/__generated__'
import MappingContainer from '@/components/rjsf/MqttTransformation/components/MappingContainer.tsx'

interface MappingDrawerProps {
  adapterId: string
  adapterType: string
  item: SouthboundMapping
  onSubmit: (newItem: SouthboundMapping) => void
  onChange: (id: keyof SouthboundMapping, v: JsonNode | string | string[] | null) => void
  onClose: () => void
}

const MappingDrawer: FC<MappingDrawerProps> = ({ adapterId, adapterType, item, onClose, onSubmit, onChange }) => {
  const { t } = useTranslation('components')

  return (
    <Drawer isOpen={true} placement="right" size="full" onClose={onClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>{t('rjsf.MqttTransformationField.tabs.editor')}</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <MappingContainer
                adapterId={adapterId}
                adapterType={adapterType}
                item={item}
                onClose={onClose}
                onSubmit={onSubmit}
                onChange={onChange}
              />
            </CardBody>
          </Card>
        </DrawerBody>

        <DrawerFooter>
          <ButtonGroup>
            <Button onClick={onClose}>{t('rjsf.MqttTransformationField.actions.cancel.aria-label')}</Button>
            <Button onClick={() => onSubmit(item)} variant="primary">
              {t('rjsf.MqttTransformationField.actions.save.aria-label')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default MappingDrawer
