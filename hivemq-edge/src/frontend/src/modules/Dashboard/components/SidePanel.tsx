import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Box, Button, Card, Flex, Image, Text, useColorMode, VStack } from '@chakra-ui/react'
import { FiLogOut } from 'react-icons/fi'

import logo1 from '@/assets/edge/03-hivemq-industrial-edge-vert.svg'
import logo2 from '@/assets/edge/04-hivemq-industrial-edge-vert-neg.svg'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
import SwitchModeButton from '@/components/Chakra/SwitchModeButton.tsx'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import NotificationBadge from '@/modules/Notifications/NotificationBadge.tsx'

import NavLinksBlock from './NavLinksBlock.tsx'
import useGetNavItems from '../hooks/useGetNavItems.tsx'

const SidePanel: FC = () => {
  const { t } = useTranslation()
  const { data: configuration } = useGetConfiguration()
  const { data: items } = useGetNavItems()
  const auth = useAuth()
  const navigate = useNavigate()
  const { colorMode } = useColorMode()

  return (
    <nav>
      <Flex
        as={Card}
        sx={{
          backgroundColor: '#f5f5f5',
          _dark: {
            backgroundColor: '#2D3748',
          },
        }}
        flexDirection="column"
        w={256}
        h={'100%'}
        overflow={'auto'}
      >
        <VStack p={4} mb={4}>
          <Image src={colorMode === 'light' ? logo1 : logo2} alt={t('branding.company') as string} boxSize="100px" />
          {configuration && (
            <Text data-testid="edge-release" fontSize="xs" textAlign={'center'}>
              [ {configuration.environment?.properties?.version} ]
            </Text>
          )}
        </VStack>

        <Box m={4} mb={8}>
          <NotificationBadge />
        </Box>

        <Flex flexDirection="column" flex={1}>
          {items.map(({ title, items }) => (
            <NavLinksBlock key={title} title={title} items={items} />
          ))}
        </Flex>

        <Flex flexDirection="column" flex={1}></Flex>
        <Flex p={4} flexDirection={'row'} alignItems={'center'} justifyContent={'space-between'} ml={2}>
          <Button leftIcon={<FiLogOut />} variant="link" onClick={() => auth.logout(() => navigate('/login'))}>
            {t('translation:action.logout')}
          </Button>
          <SwitchModeButton size={'sm'} />
        </Flex>
      </Flex>
    </nav>
  )
}

export default SidePanel
