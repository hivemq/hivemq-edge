import { FC } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { Flex } from '@chakra-ui/react'
import { SkipNavContent, SkipNavLink } from '@chakra-ui/skip-nav'

import SidePanel from './components/SidePanel.tsx'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../Auth/hooks/useAuth.ts'

const Dashboard: FC = () => {
  const { credentials } = useAuth()
  const { t } = useTranslation()

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
