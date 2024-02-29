import { FC } from 'react'
import { Image, Badge, Box, Flex, Highlight, Link, Text, HighlightProps, Skeleton } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ProtocolAdapter } from '@/api/__generated__'

interface AdapterTypeSummaryProps {
  adapter: ProtocolAdapter
  searchQuery?: string | null
  isLoading?: boolean
}

const AdapterHighlight: FC<HighlightProps> = (props) => (
  <Highlight query={props.query} styles={{ rounded: 'full', bg: 'orange.100' }}>
    {props.children}
  </Highlight>
)

const AdapterTypeSummary: FC<AdapterTypeSummaryProps> = ({ adapter, searchQuery, isLoading }) => {
  const { t } = useTranslation()

  return (
    <Flex m={0}>
      <Skeleton isLoaded={!isLoading}>
        <Image boxSize="100px" objectFit="scale-down" src={adapter.logoUrl} aria-label={adapter.id} />
      </Skeleton>
      <Box ml="3">
        <Skeleton isLoaded={!isLoading} as={'p'}>
          <Text fontWeight="bold" data-testid={'protocol-name'} id={`adapter-${adapter.id}`} as={'span'}>
            <AdapterHighlight query={searchQuery || ''}>{adapter.name || ''}</AdapterHighlight>
          </Text>
          <Badge ml="1" colorScheme="brand" variant={'solid'} data-testid={'protocol-version'}>
            {adapter.version}
          </Badge>
        </Skeleton>
        <Skeleton isLoaded={!isLoading} mt={1}>
          <Text fontSize="sm" data-testid={'protocol-type'}>
            {t('protocolAdapter.overview.type')} {adapter.protocol}
          </Text>
          <Text fontSize="sm" data-testid={'protocol-author'}>
            {t('protocolAdapter.overview.author')} {adapter.author}
          </Text>
          <Text fontSize="sm" data-testid={'protocol-description'}>
            <AdapterHighlight query={searchQuery || ''}>{adapter.description || ''}</AdapterHighlight>
          </Text>
        </Skeleton>
        <Box mt={2}>
          <Skeleton isLoaded={!isLoading}>
            <Link href={adapter.url} isExternal>
              {t('protocolAdapter.overview.documentation')} <ExternalLinkIcon mx={1} mb={1} />
            </Link>
          </Skeleton>
        </Box>
      </Box>
    </Flex>
  )
}

export default AdapterTypeSummary
