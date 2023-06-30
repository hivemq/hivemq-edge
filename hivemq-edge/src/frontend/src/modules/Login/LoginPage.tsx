import { FC } from 'react'
import { Flex, Stack } from '@chakra-ui/react'

import Login from './components/Login.tsx'
import EdgeAside from './components/EdgeAside.tsx'

const LoginPage: FC = () => {
  return (
    <Stack minH={'100vh'} direction={{ base: 'column', md: 'row' }}>
      <Flex flex={{ base: '0', md: '1' }} bg={'brand.500'} align={'center'} justify={'center'}>
        <EdgeAside />
      </Flex>
      <Flex p={8} flex={1} align={'center'} justify={'center'}>
        <Login />
      </Flex>
    </Stack>
  )
}

export default LoginPage
