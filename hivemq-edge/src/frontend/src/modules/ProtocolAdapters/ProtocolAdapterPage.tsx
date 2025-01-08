import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Flex } from '@chakra-ui/react'
import { BiAddToQueue, BiArrowBack } from 'react-icons/bi'

import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import PageContainer from '@/components/PageContainer.tsx'

const ProtocolAdapterPage: FC = () => {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const navigate = useNavigate()

  const isMainPage = pathname === '/protocol-adapters'

  return (
    <PageContainer
      title={t('protocolAdapter.title')}
      subtitle={t('protocolAdapter.description')}
      cta={
        <Flex height="100%" justifyContent="flex-end" alignItems="flex-end" pb={6}>
          {isMainPage && (
            <Button
              leftIcon={<BiAddToQueue />}
              onClick={() => navigate('/protocol-adapters/catalog')}
              variant="primary"
            >
              {t('protocolAdapter.action.goCatalog')}
            </Button>
          )}
          {!isMainPage && (
            <Button leftIcon={<BiArrowBack />} onClick={() => navigate('/protocol-adapters', { replace: true })}>
              {t('protocolAdapter.action.goAdapter')}
            </Button>
          )}
        </Flex>
      }
    >
      <SuspenseOutlet />
    </PageContainer>
  )
}

export default ProtocolAdapterPage
