import { FC, Suspense } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, Outlet } from 'react-router-dom'
import { AbsoluteCenter, Box, Flex } from '@chakra-ui/react'
import { SkipNavContent, SkipNavLink } from '@chakra-ui/skip-nav'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import SidePanel from './components/SidePanel.tsx'
import { useAuth } from '../Auth/hooks/useAuth.ts'

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
          <Suspense
            fallback={
              <AbsoluteCenter axis="both">
                <LoaderSpinner />
              </AbsoluteCenter>
            }
          >
            <Outlet />
          </Suspense>
        </Flex>
      </Flex>
    </>
  )
}

export default Dashboard
