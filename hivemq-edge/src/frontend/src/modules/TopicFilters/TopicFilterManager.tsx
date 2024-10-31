import { type FC, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDisclosure } from '@chakra-ui/react'

import { useTopicFilterManager } from '@/api/hooks/useTopicFilters/useTopicFilterManager.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'

const TopicFilterManager: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { isLoading, isError } = useTopicFilterManager()

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  return (
    <ExpandableDrawer
      header={t('topicFilter.manager.header')}
      isOpen={isOpen}
      onClose={handleClose}
      closeOnOverlayClick={true}
    >
      {isLoading && <LoaderSpinner />}
      {isError && <ErrorMessage message={t('topicFilter.error.loading')} />}
    </ExpandableDrawer>
  )
}

export default TopicFilterManager
