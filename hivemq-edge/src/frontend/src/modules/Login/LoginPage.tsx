import { FC } from 'react'
import { Flex, Stack } from '@chakra-ui/react'

import { useGetConfiguration } from '@/api/hooks/useGatewayPortal/useGetConfiguration.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import Login from './components/Login.tsx'
import EdgeAside from './components/EdgeAside.tsx'

const LoginPage: FC = () => {
  const { data, isLoading, isError, error } = useGetConfiguration()

  return (
    <Stack minH={'100vh'} direction={{ base: 'column', md: 'row' }}>
      <Flex flex={{ base: '0', md: '1' }} bg={'brand.500'} align={'center'} justify={'center'}>
        <EdgeAside />
      </Flex>
      <Flex p={8} flex={1} align={'center'} justify={'center'}>
        {isLoading && <LoaderSpinner />}
        {isError && error && <ErrorMessage type={error?.statusText} message={error?.body?.title} />}
        {!isError && !isLoading && <Login first={data?.firstUseInformation} />}
      </Flex>
    </Stack>
  )
}

export default LoginPage
