import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { Box } from '@chakra-ui/react'

import PageContainer from '@/components/PageContainer.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.tsx'
import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

const DataHubPage: FC = () => {
  const { t } = useTranslation('datahub')
  const hasDataHub = useGetCapability(CAPABILITY.DATAHUB)

  return (
    <PageContainer title={t('page.title') as string} subtitle={t('page.description') as string}>
      {hasDataHub && <Outlet />}
      {!hasDataHub && (
        <Box width="100%">
          <WarningMessage
            image={AdapterEmptyLogo}
            title={t('error.notActivated.title') as string}
            prompt={t('error.notActivated.description') as string}
            alt={t('brand.extension')}
            mt={10}
          />
        </Box>
      )}
    </PageContainer>
  )
}

export default DataHubPage
