import { FC } from 'react'
import { Box, Flex, Image, Stack } from '@chakra-ui/react'

import logo from '@/assets/edge/01-hivemq-industrial-edge.svg'
import bgImage from '@/assets/app/background-sidepanel.svg'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import Login from './components/Login.tsx'
import EdgeAside from './components/EdgeAside.tsx'
import { useTranslation } from 'react-i18next'

const LoginPage: FC = () => {
  const { data, isLoading, isError, error } = useGetConfiguration()
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
            {isError && error && <ErrorMessage type={error?.statusText} message={error?.body?.title} />}
            {!isError && !isLoading && <Login first={data?.firstUseInformation} />}
          </div>
          <Box flex={1}></Box>
        </Flex>
      </Stack>
    </main>
  )
}

export default LoginPage
