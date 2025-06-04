import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Flex } from '@chakra-ui/react'
import { BiAddToQueue } from 'react-icons/bi'
import { useNavigate } from 'react-router-dom'

import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import PageContainer from '@/components/PageContainer.tsx'
import Bridges from '@/modules/Bridges/Bridges.tsx'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { BridgeProvider } from '@/modules/Bridges/hooks/BridgeProvider.tsx'

const BridgePage: FC = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { isLoading, isError } = useListBridges()

  return (
    <PageContainer
      title={t('bridge.title')}
      subtitle={t('bridge.description')}
      cta={
        <Flex height="100%" justifyContent="flex-end" alignItems="flex-end" pb={6}>
          <Button
            leftIcon={<BiAddToQueue />}
            onClick={() => navigate('/mqtt-bridges/new')}
            isDisabled={isLoading || isError}
            variant="primary"
          >
            {t('bridge.action.add')}
          </Button>
        </Flex>
      }
    >
      <BridgeProvider>
        <Bridges />
        <SuspenseOutlet />
      </BridgeProvider>
    </PageContainer>
  )
}

export default BridgePage
