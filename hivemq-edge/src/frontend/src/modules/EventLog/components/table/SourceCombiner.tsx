import type { FC } from 'react'
import { Link as ReactRouterLink } from 'react-router-dom'
import { Icon, Link as ChakraLink, Text } from '@chakra-ui/react'

import type { TypeIdentifier } from '@/api/__generated__'
import { useGetCombiner } from '@/api/hooks/useCombiners'
import { HqCombiner } from '@/components/Icons'

interface SourceLinkProps {
  source: TypeIdentifier
}

export const SourceCombiner: FC<SourceLinkProps> = ({ source }) => {
  const { data, isSuccess } = useGetCombiner(source.identifier as string)

  const isExisting = isSuccess && Boolean(data)
  if (!isExisting) return <Text>{source?.identifier}</Text>

  const visibleName = isSuccess && data.name ? data?.name : source?.identifier
  return (
    <ChakraLink
      as={ReactRouterLink}
      to={`/workspace/combiner/${source.identifier}`}
      whiteSpace="nowrap"
      display="inline-flex"
      alignItems="center"
    >
      <Icon as={HqCombiner} mr={2} data-type="DATA_COMBINING" />
      {visibleName}
    </ChakraLink>
  )
}
