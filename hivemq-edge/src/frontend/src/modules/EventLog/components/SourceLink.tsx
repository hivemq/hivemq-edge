import { FC } from 'react'
import { Link as ReactRouterLink } from 'react-router-dom'
import { Box, Icon, Link as ChakraLink } from '@chakra-ui/react'
import { PiBridgeThin, PiPlugsConnectedFill, PiUserFill } from 'react-icons/pi'

import { TypeIdentifier } from '@/api/__generated__'
import { MdOutlineEventNote } from 'react-icons/md'

interface SourceLinkProps {
  event: TypeIdentifier | undefined
}

const SourceLink: FC<SourceLinkProps> = ({ event }) => {
  const IconComponent = {
    [TypeIdentifier.type.ADAPTER]: <Icon as={PiPlugsConnectedFill} mr={2} />,
    [TypeIdentifier.type.ADAPTER_TYPE]: <Icon as={PiPlugsConnectedFill} mr={2} />,
    [TypeIdentifier.type.BRIDGE]: <Icon as={PiBridgeThin} fontSize={'20px'} mr={2} />,
    [TypeIdentifier.type.EVENT]: <Icon as={MdOutlineEventNote} mr={2} />,
    [TypeIdentifier.type.USER]: <Icon as={PiUserFill} mr={2} />,
  }

  const navRoute = {
    [TypeIdentifier.type.ADAPTER]: (id: string | undefined) => `/protocol-adapters/${id}`,
    [TypeIdentifier.type.ADAPTER_TYPE]: undefined,
    [TypeIdentifier.type.BRIDGE]: (id: string | undefined) => `/mqtt-bridges/${id}`,
    [TypeIdentifier.type.EVENT]: undefined,
    [TypeIdentifier.type.USER]: undefined,
  }

  if (!event?.type || !navRoute[event?.type])
    return (
      <Box whiteSpace={'nowrap'} display={'inline-flex'}>
        {event?.identifier}
      </Box>
    )

  return (
    <ChakraLink
      as={ReactRouterLink}
      to={navRoute[event.type]?.(event.identifier)}
      whiteSpace={'nowrap'}
      display={'inline-flex'}
    >
      {event.type && IconComponent[event.type]}
      {event.identifier}
    </ChakraLink>
  )
}

export default SourceLink
