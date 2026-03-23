import { type FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, useDisclosure } from '@chakra-ui/react'
import { LuPlus } from 'react-icons/lu'

import type { TopicBufferSubscription } from '@/api/__generated__'
import PageContainer from '@/components/PageContainer.tsx'

import TopicBufferTable from './components/TopicBufferTable.tsx'
import TopicBufferEditorDrawer from './components/TopicBufferEditorDrawer.tsx'

const TopicBufferPage: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [selected, setSelected] = useState<TopicBufferSubscription | undefined>(undefined)

  const handleEdit = (item: TopicBufferSubscription) => {
    setSelected(item)
    onOpen()
  }

  const handleAdd = () => {
    setSelected(undefined)
    onOpen()
  }

  const handleClose = () => {
    setSelected(undefined)
    onClose()
  }

  return (
    <PageContainer
      title={t('topicBuffer.title')}
      subtitle={t('topicBuffer.description')}
      cta={
        <Button leftIcon={<LuPlus />} onClick={handleAdd} data-testid="topic-buffer-add">
          {t('topicBuffer.action.add')}
        </Button>
      }
    >
      <TopicBufferTable onEdit={handleEdit} />
      <TopicBufferEditorDrawer isOpen={isOpen} onClose={handleClose} item={selected} />
    </PageContainer>
  )
}

export default TopicBufferPage
