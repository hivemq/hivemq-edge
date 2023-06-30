import { FC } from 'react'
import { Box, Flex, List, ListItem, Text } from '@chakra-ui/react'

import { NavLink } from './NavLink.tsx'
import { NavLinksBlockType } from '../types.ts'

const NavLinksBlock: FC<NavLinksBlockType> = ({ title, items }) => {
  return (
    <Flex flexDirection={'column'} m={0}>
      <Text m={4} my={0} fontWeight={'bold'}>
        {title}
      </Text>
      <Box pb={2} pt={2}>
        <List spacing={0}>
          {items
            .filter((e) => !e.isDisabled)
            .map((item) => (
              <ListItem key={item.label}>
                <NavLink link={item} />
              </ListItem>
            ))}
        </List>
      </Box>
    </Flex>
  )
}

export default NavLinksBlock
