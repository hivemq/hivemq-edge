import { AbsoluteCenter, Box, Flex } from '@chakra-ui/react'
import { SkipNavContent, SkipNavLink } from '@chakra-ui/skip-nav'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, Outlet } from 'react-router-dom'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import { useAuth } from '../Auth/hooks/useAuth.ts'
import SidePanel from './components/SidePanel.tsx'

const Dashboard: FC = () => {
  const { credentials, isLoading } = useAuth()
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <Box position="relative" h="100vh">
        <AbsoluteCenter p="4" axis="both">
          <LoaderSpinner />
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
      <Flex flexDirection="row" h={'100vh'} overflow={'hidden'}>
        <SidePanel />
        <Flex as={'main'} flexGrow={1} overflow={'auto'}>
          <SkipNavContent />
          <Outlet />
        </Flex>
      </Flex>
    </>
  )
}

export default Dashboard
