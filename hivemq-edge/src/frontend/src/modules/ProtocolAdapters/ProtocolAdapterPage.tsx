import { FC, Suspense, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AbsoluteCenter, Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'

import { Outlet, useLocation } from 'react-router-dom'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import PageContainer from '@/components/PageContainer.tsx'
import ProtocolAdapters from '@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'
import ProtocolIntegrationStore from '@/modules/ProtocolAdapters/components/panels/ProtocolIntegrationStore.tsx'
import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'

const ProtocolAdapterPage: FC = () => {
  const { t } = useTranslation()
  const { state } = useLocation()
  const { data: configuration } = useGetConfiguration()
  const isReturningUser = useMemo(() => {
    return configuration?.firstUseInformation?.firstUse !== true
  }, [configuration?.firstUseInformation?.firstUse])
  const [tabIndex, setTabIndex] = useState(0)

  useEffect(() => {
    setTabIndex(isReturningUser ? ProtocolAdapterTabIndex.adapters : ProtocolAdapterTabIndex.protocols)
  }, [isReturningUser])

  useEffect(() => {
    if ((state as AdapterNavigateState)?.protocolAdapterTabIndex) {
      setTabIndex(state.protocolAdapterTabIndex)
    }
  }, [state])

  const handleTabsChange = (index: number) => {
    setTabIndex(index)
  }

  return (
    <PageContainer title={t('protocolAdapter.title')} subtitle={t('protocolAdapter.description')}>
      <Tabs onChange={handleTabsChange} index={tabIndex} isLazy colorScheme="brand">
        <TabList>
          <Tab fontSize="lg" fontWeight="bold">
            {t('protocolAdapter.tabs.protocols')}
          </Tab>
          <Tab fontSize="lg" fontWeight="bold">
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
      <Suspense
        fallback={
          <AbsoluteCenter axis="both">
            <LoaderSpinner />
          </AbsoluteCenter>
        }
      >
        <Outlet />
      </Suspense>
    </PageContainer>
  )
}

export default ProtocolAdapterPage
