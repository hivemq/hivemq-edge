import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { Box } from '@chakra-ui/react'

import PageContainer from '@/components/PageContainer.tsx'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'
import LicenseWarning from '@datahub/components/helpers/LicenseWarning.tsx'

const DataHubPage: FC = () => {
  const { t } = useTranslation('datahub')
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)

  return (
    <PageContainer title={t('page.title') as string} subtitle={t('page.description') as string}>
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
