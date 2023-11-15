import { FC } from 'react'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { Button, Center, HStack, Text } from '@chakra-ui/react'

import { MainNavLinkType } from '../types.ts'

export const NavLink: FC<{ link: MainNavLinkType }> = ({ link }) => {
  const location = useLocation()

  const { href, icon, isExternal, isDisabled, isActive, label } = link
  const active = location.pathname === href || !!isActive

  // TODO[NVL] Styling should be done in a proper theme's variant
  return (
    <Button
      justifyContent={'flex-start'}
      variant={active ? 'solid' : 'ghost'}
      size="sm"
      w={'100%'}
      as={isDisabled ? undefined : RouterLink}
      to={href}
      target={isExternal ? '_blank' : undefined}
      h={'40px'}
      borderLeftColor={active ? '#FFC000' : '#f5f5f5'}
      borderLeftWidth={active ? 8 : 0}
      borderRadius={0}
    >
      <HStack spacing="3" fontSize="sm" fontWeight={active ? 'semibold' : 'medium'} ml={active ? 0 : 2}>
        <Center w="6" h="6">
          {icon}
        </Center>
        <Text as={'span'}>{label}</Text>
      </HStack>
    </Button>
  )
}
