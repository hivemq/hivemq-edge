import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box } from '@chakra-ui/react'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import PageContainer from '@/components/PageContainer.tsx'
import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import LicenseWarning from '@/modules/Pulse/components/activation/LicenseWarning.tsx'
import AssetsTable from '@/modules/Pulse/components/assets/AssetsTable.tsx'

const PulsePage: FC = () => {
  const { t } = useTranslation()
  const { data: hasPulse } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  return (
    <PageContainer title={t('pulse.title')} subtitle={t('pulse.description')}>
      {!hasPulse && (
        <Box width="100%" mt={20}>
          <LicenseWarning />
        </Box>
      )}
      {hasPulse && <AssetsTable />}
      <SuspenseOutlet />
    </PageContainer>
  )
}

export default PulsePage
