import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'

import { Outlet } from 'react-router-dom'

import PageContainer from '@/components/PageContainer.tsx'
import ProtocolAdapters from '@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'
import ProtocolIntegrationStore from '@/modules/ProtocolAdapters/components/panels/ProtocolIntegrationStore.tsx'

const ProtocolAdapterPage: FC = () => {
  const { t } = useTranslation()
  return (
    <PageContainer title={t('protocolAdapter.title') as string} subtitle={t('protocolAdapter.description') as string}>
      <Tabs isLazy>
        <TabList>
          <Tab fontSize="lg" fontWeight={'bold'}>
            {t('protocolAdapter.tabs.protocols')}
          </Tab>
          <Tab fontSize="lg" fontWeight={'bold'}>
            {t('protocolAdapter.tabs.adapters')}
          </Tab>
        </TabList>

        <TabPanels>
          <TabPanel>
            <ProtocolIntegrationStore />
          </TabPanel>
          <TabPanel>
            <ProtocolAdapters />
          </TabPanel>
        </TabPanels>
      </Tabs>
      <Outlet />
    </PageContainer>
  )
}

export default ProtocolAdapterPage
