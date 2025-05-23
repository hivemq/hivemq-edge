import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  type UseDisclosureProps,
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useDisclosure,
} from '@chakra-ui/react'

import type { DomainTag } from '@/api/__generated__'
import { TagSchemaPanel } from './TagSchemaPanel'

interface TopicSchemaDrawerProps {
  tag: DomainTag
  adapterId: string
  trigger: (disclosureProps: UseDisclosureProps) => JSX.Element
}

const TagSchemaDrawer: FC<TopicSchemaDrawerProps> = ({ tag, adapterId, trigger }) => {
  const { t } = useTranslation()
  const props = useDisclosure()

  const onHandleSubmit = () => {
    props.onClose()
  }

  return (
    <>
      {trigger(props)}
      <Drawer
        isOpen={props.isOpen}
        placement="right"
        size="lg"
        onClose={props.onClose}
        closeOnOverlayClick={false}
        id="tag-schema"
      >
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>
            <Text>{t('device.drawer.schema.header')}</Text>
          </DrawerHeader>

          <DrawerBody>
            <TagSchemaPanel tag={tag} adapterId={adapterId} />
          </DrawerBody>

          <DrawerFooter>
            <Button onClick={onHandleSubmit}>{t('device.drawer.schema.action.close')}</Button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default TagSchemaDrawer
