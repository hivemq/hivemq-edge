import { Box, Flex, Image, Stack } from '@chakra-ui/react'
import { FC } from 'react'

import bgImage from '@/assets/app/background-sidepanel.svg'
import logo from '@/assets/edge/01-hivemq-industrial-edge.svg'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import { useTranslation } from 'react-i18next'
import EdgeAside from './components/EdgeAside.tsx'
import Login from './components/Login.tsx'

const LoginPage: FC = () => {
  const { data, isLoading, error } = useGetConfiguration()
  const { t } = useTranslation()

  return (
    <main>
      <Stack minH={'100vh'} direction={{ base: 'column', md: 'row' }}>
        <Flex
          flex={{ base: '0', md: '3' }}
          bg={'brand.500'}
          align={'center'}
          justify={'center'}
          backgroundImage={`url(${bgImage})`}
        >
          <EdgeAside />
        </Flex>
        <Flex p={8} flex={2} align={'center'} justify={'center'} flexDirection={'column'}>
          <Box flex={1} width={'100%'} display={{ base: 'none', md: 'inherit' }}>
            <Image src={logo} alt={t('branding.appName') as string} boxSize={'50px'} w={'initial'} />
          </Box>
          <div>
            {isLoading && <LoaderSpinner />}
            {!isLoading && <Login first={data?.firstUseInformation} preLoadError={error} />}
          </div>
          <Box flex={1} />
        </Flex>
      </Stack>
    </main>
  )
}

export default LoginPage
