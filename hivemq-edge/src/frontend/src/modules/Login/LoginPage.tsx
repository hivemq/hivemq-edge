import { FC } from 'react'
import { Box, Flex, Image, Stack, useColorMode, useColorModeValue } from '@chakra-ui/react'

import logoLight from '@/assets/edge/01-hivemq-industrial-edge.svg'
import logoDark from '@/assets/edge/02-hivemq-industrial-edge-neg.svg'
import bgImage from '@/assets/app/background-sidepanel.svg'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import Login from './components/Login.tsx'
import EdgeAside from './components/EdgeAside.tsx'
import { useTranslation } from 'react-i18next'

const LoginPage: FC = () => {
  const { data, isLoading, error } = useGetConfiguration()
  const { t } = useTranslation()
  const { colorMode } = useColorMode()
  const bgColour = useColorModeValue('brand.500', 'brand.700')

  return (
    <main>
      <Stack minH={'100vh'} direction={{ base: 'column', md: 'row' }}>
        <Flex
          flex={{ base: '0', md: '3' }}
          bg={bgColour}
          align={'center'}
          justify={'center'}
          backgroundImage={`url(${bgImage})`}
        >
          <EdgeAside />
        </Flex>
        <Flex p={8} flex={2} align={'center'} justify={'center'} flexDirection={'column'}>
          <Box flex={1} width={'100%'} display={{ base: 'none', md: 'inherit' }}>
            <Image
              src={colorMode === 'light' ? logoLight : logoDark}
              alt={t('branding.appName') as string}
              boxSize={'50px'}
              w={'initial'}
            />
          </Box>
          <div>
            {isLoading && <LoaderSpinner />}
            {!isLoading && <Login first={data?.firstUseInformation} preLoadError={error} />}
          </div>
          <Box flex={1}></Box>
        </Flex>
      </Stack>
    </main>
  )
}

export default LoginPage
