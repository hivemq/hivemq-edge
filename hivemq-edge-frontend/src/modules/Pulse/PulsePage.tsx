import { Box } from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import PageContainer from '@/components/PageContainer.tsx'
import LicenseWarning from '@/modules/Pulse/components/activation/LicenseWarning.tsx'

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
    </PageContainer>
  )
}

export default PulsePage
