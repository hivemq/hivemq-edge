import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useDisclosure } from '@chakra-ui/react'

import PageContainer from '@/components/PageContainer.tsx'

import EventDrawer from './components/panel/EventDrawer.tsx'
import EventLogTable from './components/table/EventLogTable.tsx'

const EvenLogPage: FC = () => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { t } = useTranslation()

  const handleEditorOnClose = () => {
    onClose()
  }

  const handleEditorOpen = () => {
    onOpen()
  }

  return (
    <PageContainer title={t('eventLog.title') as string} subtitle={t('eventLog.description') as string}>
      <EventLogTable onOpen={handleEditorOpen} />
      <EventDrawer isOpen={isOpen} onClose={handleEditorOnClose} event={undefined} />
    </PageContainer>
  )
}

export default EvenLogPage
