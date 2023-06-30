import { FC } from 'react'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { Button, Center, HStack, Text } from '@chakra-ui/react'

import { MainNavLinkType } from '../types.ts'

export const NavLink: FC<{ link: MainNavLinkType }> = ({ link }) => {
  const location = useLocation()

  const { href, icon, isExternal, isDisabled, isActive, label } = link
  const active = location.pathname === href || !!isActive

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
      borderLeftColor={active ? '#FFC000' : 'white'}
      borderLeftWidth={8}
      borderRadius={0}
      // _hover={!isExternal ? { borderLeftColor: '#FFC000' } : {}}
    >
      <HStack spacing="3" fontSize="sm" fontWeight={active ? 'semibold' : 'medium'}>
        <Center w="6" h="6">
          {icon}
        </Center>
        <Text as={'span'}>{label}</Text>
      </HStack>
    </Button>
  )
}
