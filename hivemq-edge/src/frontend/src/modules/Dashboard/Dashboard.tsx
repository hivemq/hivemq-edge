import { FC } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { AbsoluteCenter, Box, Flex, Spinner } from '@chakra-ui/react'
import { SkipNavContent, SkipNavLink } from '@chakra-ui/skip-nav'

import SidePanel from './components/SidePanel.tsx'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../Auth/hooks/useAuth.ts'

const Dashboard: FC = () => {
  const { credentials, isLoading } = useAuth()
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <Box position="relative" h="100vh">
        <AbsoluteCenter p="4" axis="both">
          <Spinner thickness="4px" speed="0.65s" emptyColor="gray.200" color="blue.500" size="xl" />
        </AbsoluteCenter>
      </Box>
    )
  }
  if (!credentials) {
    return <Navigate to="/login" state={{ from: location }} />
  }

  return (
    <>
      <SkipNavLink>{t('translation:action.skipNavLink')}</SkipNavLink>
      <main>
        <Flex flexDirection="row" h={'100vh'}>
          <SidePanel />
          <Flex w={'100vw'} flexGrow={1}>
            <SkipNavContent />
            <Outlet />
          </Flex>
        </Flex>
      </main>
    </>
  )
}

export default Dashboard
