import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { DateTime } from 'luxon'

import {
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerContent,
  DrawerCloseButton,
  Text,
  Card,
  CardHeader,
  CardBody,
  Grid,
  GridItem,
  VStack,
  Code,
  Badge,
} from '@chakra-ui/react'

import { Event, Payload } from '@/api/__generated__'
import SeverityBadge from '@/modules/EventLog/components/SeverityBadge.tsx'
import { prettifyXml, prettyJSON } from '@/modules/EventLog/utils/payload-utils.ts'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'

import SourceLink from '../SourceLink.tsx'

interface BridgeMainDrawerProps {
  event: Event
  isOpen: boolean

  onClose: () => void
}

const EventDrawer: FC<BridgeMainDrawerProps> = ({ event, isOpen, onClose }) => {
  const { t } = useTranslation()

  const isJSON = event.payload?.contentType === Payload.contentType.JSON
  const isXML = event.payload?.contentType === Payload.contentType.XML

  const JSONFormat = isJSON && prettyJSON(event.payload?.content)
  const XMLFormat = isXML && prettifyXml(event.payload?.content)

  return (
    <>
      <Drawer
        variant={'hivemq'}
        closeOnOverlayClick={true}
        size={'lg'}
        isOpen={isOpen}
        placement="right"
        onClose={onClose}
      >
        <DrawerContent aria-label={t('bridge.drawer.label') as string}>
          <DrawerCloseButton />
          <DrawerHeader id={'bridge-form-header'}>{t('eventLog.panel.title')}</DrawerHeader>

          <DrawerBody>
            <VStack gap={2}>
              <Card w={'100%'}>
                <CardHeader>
                  <SeverityBadge event={event} />
                </CardHeader>
                <CardBody>
                  <Grid templateColumns="repeat(2, 1fr)" gap={6}>
                    <GridItem data-testid={'event-title-created'}>
                      <Text>{t('eventLog.table.header.created')}</Text>
                    </GridItem>
                    <GridItem data-testid={'event-value-created'}>
                      <DateTimeRenderer date={DateTime.fromISO(event?.created || '')} />
                    </GridItem>
                    <GridItem data-testid={'event-title-source'}>
                      <Text>{t('eventLog.table.header.source')}</Text>
                    </GridItem>
                    <GridItem data-testid={'event-value-source'}>
                      <SourceLink source={event?.source} />
                    </GridItem>
                    <GridItem data-testid={'event-title-associatedObject'}>
                      <Text>{t('eventLog.table.header.associatedObject')}</Text>
                    </GridItem>
                    <GridItem data-testid={'event-value-associatedObject'}>
                      <SourceLink source={event?.associatedObject} />
                    </GridItem>
                  </Grid>
                </CardBody>
              </Card>

              <Card w={'100%'}>
                <CardBody data-testid={'event-value-message'}>{event.message}</CardBody>
              </Card>

              {event.payload && (
                <Card w={'100%'}>
                  <CardHeader data-testid={'event-title-payload'}>
                    {t('eventLog.table.header.payload')}
                    <Badge variant="outline" mx={2}>
                      {event.payload?.contentType}
                    </Badge>
                  </CardHeader>
                  <CardBody pt={0}>
                    <Code
                      w={'100%'}
                      p={2}
                      whiteSpace={'pre-wrap'}
                      overflow={'auto'}
                      sx={{ textWrap: 'nowrap' }}
                      maxH={400}
                    >
                      {JSONFormat && JSONFormat}
                      {XMLFormat && XMLFormat}
                      {!isJSON && !isXML && event.payload?.content}
                    </Code>
                  </CardBody>
                </Card>
              )}
            </VStack>
          </DrawerBody>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default EventDrawer
