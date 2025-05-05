import type { FC } from 'react'
import { Link as ReactRouterLink } from 'react-router-dom'
import { Box, Icon, Link as ChakraLink } from '@chakra-ui/react'
import { PiBridgeThin, PiPlugsConnectedFill, PiUserFill } from 'react-icons/pi'
import { MdOutlineEventNote } from 'react-icons/md'

import { TypeIdentifier } from '@/api/__generated__'
import type { AdapterNavigateState } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import { SourceCombiner } from './table/SourceCombiner'

interface SourceLinkProps {
  source: TypeIdentifier | undefined
  type?: TypeIdentifier | undefined
}

const SourceLink: FC<SourceLinkProps> = ({ source, type }) => {
  let icon: JSX.Element | undefined
  let to: string | undefined
  let state: AdapterNavigateState | undefined

  switch (source?.type) {
    case TypeIdentifier.type.ADAPTER:
      icon = <Icon as={PiPlugsConnectedFill} mr={2} data-type={TypeIdentifier.type.ADAPTER} />
      state = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.ADAPTERS,
        protocolAdapterType: type?.identifier,
      }
      to = `/protocol-adapters/edit/${type?.identifier}/${source.identifier}`
      break
    case TypeIdentifier.type.ADAPTER_TYPE:
      icon = <Icon as={PiPlugsConnectedFill} mr={2} data-type={TypeIdentifier.type.ADAPTER_TYPE} />
      break
    case TypeIdentifier.type.BRIDGE:
      icon = <Icon as={PiBridgeThin} fontSize="20px" mr={2} data-type={TypeIdentifier.type.BRIDGE} />
      to = `/mqtt-bridges/${source.identifier}`
      break
    case TypeIdentifier.type.EVENT:
      icon = <Icon as={MdOutlineEventNote} mr={2} data-type={TypeIdentifier.type.EVENT} />
      break
    case TypeIdentifier.type.USER:
      icon = <Icon as={PiUserFill} mr={2} data-type={TypeIdentifier.type.USER} />
      break
    case TypeIdentifier.type.DATA_COMBINING:
    case TypeIdentifier.type.COMBINER:
      return <SourceCombiner source={source} />
    default:
      break
  }

  if (!to)
    return (
      <Box whiteSpace="nowrap" display="inline-flex">
        {source?.identifier}
      </Box>
    )

  return (
    <ChakraLink
      as={ReactRouterLink}
      to={to}
      state={state}
      whiteSpace="nowrap"
      display="inline-flex"
      alignItems="center"
    >
      {Boolean(icon) && icon}
      {source?.identifier}
    </ChakraLink>
  )
}

export default SourceLink
