import type { FC } from 'react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Flex, Image, Stack, useColorMode, useColorModeValue } from '@chakra-ui/react'

import logoLight from '@/assets/edge/01-hivemq-industrial-edge.svg'
import logoDark from '@/assets/edge/02-hivemq-industrial-edge-neg.svg'
import bgImage from '@/assets/app/background-sidepanel.svg'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import PreLoginNoticeForm from '@/modules/Login/components/PreLoginNoticeForm.tsx'
import EdgeAside from '@/modules/Login/components/EdgeAside.tsx'
import Login from '@/modules/Login/components/Login.tsx'

const LoginPage: FC = () => {
  const { data, isLoading, error } = useGetConfiguration()
  const { t } = useTranslation()
  const { colorMode } = useColorMode()
  const bgColour = useColorModeValue('brand.500', 'brand.700')
  const [acceptNotice, setAcceptNotice] = useState<boolean | null>(null)

  const showNotice = useMemo(() => {
    if (isLoading) return undefined
    if (!data) return undefined

    if (data.preLoginNotice?.enabled && !acceptNotice) return data.preLoginNotice
    return undefined
  }, [acceptNotice, data, isLoading])

  return (
    <main>
      <Stack minH="100vh" direction={{ base: 'column', md: 'row' }}>
        <Flex
          flex={{ base: '0', md: '3' }}
          bg={bgColour}
          align="center"
          justify="center"
          backgroundImage={`url(${bgImage})`}
        >
          <EdgeAside />
        </Flex>
        <Flex p={8} flex={2} align="center" justify="center" flexDirection="column">
          <Box flex={1} width="100%" display={{ base: 'none', md: 'inherit' }}>
            <Image
              src={colorMode === 'light' ? logoLight : logoDark}
              alt={t('branding.appName')}
              boxSize="50px"
              w="initial"
            />
          </Box>
          <div>
            {isLoading && <LoaderSpinner />}
            {error && (
              <ErrorMessage
                type={t('login.error.noConfiguration.title')}
                message={t('login.error.noConfiguration.description')}
                stack={error}
              />
            )}
            {showNotice && (
              <PreLoginNoticeForm
                notice={showNotice}
                onAccept={() => {
                  setAcceptNotice(true)
                }}
                forceReading={Boolean(showNotice?.consent)}
              />
            )}
            {!showNotice && data && <Login first={data?.firstUseInformation} preLoadError={error} />}
          </div>
          <Box flex={1}></Box>
        </Flex>
      </Stack>
    </main>
  )
}

export default LoginPage
