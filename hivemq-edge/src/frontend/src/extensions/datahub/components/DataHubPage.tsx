import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useLocation } from 'react-router-dom'
import { Box, Flex } from '@chakra-ui/react'

import PageContainer from '@/components/PageContainer.tsx'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'
import LicenseWarning from '@datahub/components/helpers/LicenseWarning.tsx'
import DraftCTA from '@datahub/components/helpers/DraftCTA.tsx'

const DataHubPage: FC = () => {
  const { t } = useTranslation('datahub')
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)
  const { pathname } = useLocation()

  const isMainPage = pathname === '/datahub'

  return (
    <PageContainer
      title={t('page.title') as string}
      subtitle={t('page.description') as string}
      cta={
        isMainPage &&
        hasDataHub && (
          <Flex height="100%" justifyContent="flex-end" alignItems="flex-end" pb={6}>
            <DraftCTA />
          </Flex>
        )
      }
    >
      {hasDataHub && <Outlet />}
      {!hasDataHub && (
        <Box width="100%" mt={20}>
          <LicenseWarning />
        </Box>
      )}
    </PageContainer>
  )
}

export default DataHubPage
