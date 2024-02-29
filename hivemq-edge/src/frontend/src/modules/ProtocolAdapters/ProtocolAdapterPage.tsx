import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'

import { Outlet, useLocation } from 'react-router-dom'

import PageContainer from '@/components/PageContainer.tsx'
import ProtocolAdapters from '@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'
import ProtocolIntegrationStore from '@/modules/ProtocolAdapters/components/panels/ProtocolIntegrationStore.tsx'
import { AdapterNavigateState } from '@/modules/ProtocolAdapters/types.ts'

const ProtocolAdapterPage: FC = () => {
  const { t } = useTranslation()
  const { state } = useLocation()
  const [tabIndex, setTabIndex] = useState(0)

  useEffect(() => {
    if ((state as AdapterNavigateState)?.protocolAdapterTabIndex) {
      setTabIndex(state.protocolAdapterTabIndex)
    }
  }, [state])

  const handleTabsChange = (index: number) => {
    setTabIndex(index)
  }

  return (
    <PageContainer title={t('protocolAdapter.title') as string} subtitle={t('protocolAdapter.description') as string}>
      <Tabs onChange={handleTabsChange} index={tabIndex} isLazy colorScheme={'brand'}>
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
