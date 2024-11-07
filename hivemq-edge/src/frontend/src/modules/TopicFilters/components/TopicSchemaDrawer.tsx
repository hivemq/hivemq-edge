import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useDisclosure,
  UseDisclosureProps,
} from '@chakra-ui/react'

import { TopicFilter } from '@/api/__generated__'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import SchemaManager from '@/modules/TopicFilters/components/SchemaManager.tsx'

interface TopicSchemaDrawerProps {
  topicFilter: TopicFilter
  trigger: (disclosureProps: UseDisclosureProps) => JSX.Element
}

// TODO[NVL] Too similar to ArrayItemDrawer; combine?
const TopicSchemaDrawer: FC<TopicSchemaDrawerProps> = ({ topicFilter, trigger }) => {
  const { t } = useTranslation()
  const props = useDisclosure()
  const { isOpen, onClose } = props

  const onHandleSubmit = () => {
    onClose()
  }

  return (
    <>
      {trigger(props)}
      <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} closeOnOverlayClick={false}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>
            <Text>{t('topicFilter.schema.header')}</Text>
          </DrawerHeader>

          <DrawerBody>
            <Card>
              <CardHeader>
                <Topic tagTitle={topicFilter.topicFilter} mr={3} />
              </CardHeader>
              <CardBody>
                <SchemaManager topicFilter={topicFilter} />
              </CardBody>
            </Card>
          </DrawerBody>

          <DrawerFooter>
            <Button variant="primary" onClick={onHandleSubmit}>
              {t('topicFilter.schema.submit.label')}
            </Button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default TopicSchemaDrawer
