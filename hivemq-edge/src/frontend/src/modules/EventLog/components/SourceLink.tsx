import { FC } from 'react'
import { Link as ReactRouterLink } from 'react-router-dom'
import { Box, Icon, Link as ChakraLink } from '@chakra-ui/react'
import { PiBridgeThin, PiPlugsConnectedFill, PiUserFill } from 'react-icons/pi'
import { MdOutlineEventNote } from 'react-icons/md'

import { TypeIdentifier } from '@/api/__generated__'
import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'

interface SourceLinkProps {
  source: TypeIdentifier | undefined
  type?: TypeIdentifier | undefined
}

interface LinkWrapperProps {
  Icon: JSX.Element
  To?: (id: string) => string | undefined
  State?: (id: string) => AdapterNavigateState
}

const LinkWrapper: Record<TypeIdentifier.type, LinkWrapperProps> = {
  [TypeIdentifier.type.ADAPTER]: {
    Icon: <Icon as={PiPlugsConnectedFill} mr={2} />,
    State: (id: string) => ({
      protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
      protocolAdapterType: id,
    }),
    To: (id: string) => `/protocol-adapters/${id}`,
  },
  [TypeIdentifier.type.ADAPTER_TYPE]: {
    Icon: <Icon as={PiPlugsConnectedFill} mr={2} />,
  },
  [TypeIdentifier.type.BRIDGE]: {
    Icon: <Icon as={PiBridgeThin} fontSize={'20px'} mr={2} />,
    To: (id: string) => `/mqtt-bridges/${id}`,
  },
  [TypeIdentifier.type.EVENT]: {
    Icon: <Icon as={MdOutlineEventNote} mr={2} />,
  },
  [TypeIdentifier.type.USER]: {
    Icon: <Icon as={PiUserFill} mr={2} />,
  },
}

const SourceLink: FC<SourceLinkProps> = ({ source, type }) => {
  const SourceType = source?.type && LinkWrapper[source?.type]

  if (!SourceType?.To)
    return (
      <Box whiteSpace={'nowrap'} display={'inline-flex'}>
        {source?.identifier}
      </Box>
    )

  return (
    <ChakraLink
      as={ReactRouterLink}
      to={source?.identifier && SourceType.To?.(source.identifier)}
      state={type?.identifier && SourceType.State?.(type?.identifier)}
      whiteSpace={'nowrap'}
      display={'inline-flex'}
    >
      {source?.type && SourceType.Icon}
      {source?.identifier}
    </ChakraLink>
  )
}

export default SourceLink
