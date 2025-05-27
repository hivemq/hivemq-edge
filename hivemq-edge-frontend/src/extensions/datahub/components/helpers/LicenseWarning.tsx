import type { FC } from 'react'
import { Circle, Flex, Heading, Image, Link } from '@chakra-ui/react'
import { Trans, useTranslation } from 'react-i18next'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

const LicenseWarning: FC = () => {
  const { t } = useTranslation('datahub')

  return (
    <Flex flexDirection="column" alignItems="center" gap={4} textAlign="center">
      <Circle size="335" bg="gray.100">
        <Image objectFit="cover" src={AdapterEmptyLogo} alt={t('brand.extension')} />
      </Circle>

      <Heading as="h2" size="md" color="gray.500">
        <Trans
          t={t}
          i18nKey="error.notActivated.title"
          components={{ 1: <Link isExternal href="https://www.hivemq.com/contact/" /> }}
        />
      </Heading>
    </Flex>
  )
}

export default LicenseWarning
