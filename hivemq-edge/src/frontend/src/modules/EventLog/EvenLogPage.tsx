import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDisclosure } from '@chakra-ui/react'

import type { Event } from '@/api/__generated__'
import PageContainer from '@/components/PageContainer.tsx'

import EventDrawer from './components/panel/EventDrawer.tsx'
import EventLogTable from './components/table/EventLogTable.tsx'

const EvenLogPage: FC = () => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { t } = useTranslation()
  const [selectedEvent, setSelectedEvent] = useState<Event | undefined>(undefined)

  const handleEditorOnClose = () => {
    setSelectedEvent(undefined)
    onClose()
  }

  const handleEditorOpen = (evt: Event) => {
    setSelectedEvent(evt)
    onOpen()
  }

  return (
    <PageContainer title={t('eventLog.title')} subtitle={t('eventLog.description')}>
      <EventLogTable onOpen={handleEditorOpen} />
      {selectedEvent && <EventDrawer isOpen={isOpen} onClose={handleEditorOnClose} event={selectedEvent} />}
    </PageContainer>
  )
}

export default EvenLogPage
