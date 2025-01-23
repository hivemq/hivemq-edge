import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { UseDisclosureProps } from '@chakra-ui/react'
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
} from '@chakra-ui/react'

import type { TopicFilter } from '@/api/__generated__'
import { Topic } from '@/components/MQTT/EntityTag.tsx'
import TopicSchemaManager from '@/modules/TopicFilters/components/TopicSchemaManager.tsx'

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
      <Drawer isOpen={isOpen} placement="right" size="lg" onClose={onClose} closeOnOverlayClick={false}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>
            <Text>{t('topicFilter.schema.header')}</Text>
          </DrawerHeader>

          <DrawerBody>
            <Card size="sm">
              <CardHeader>
                <Text as="span">{t('topicFilter.schema.title')}</Text>{' '}
                <Topic tagTitle={topicFilter.topicFilter} mr={3} />
              </CardHeader>
              <CardBody>
                <TopicSchemaManager topicFilter={topicFilter} />
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
