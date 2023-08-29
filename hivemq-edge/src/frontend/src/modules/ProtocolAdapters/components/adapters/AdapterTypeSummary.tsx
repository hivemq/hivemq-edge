import { FC } from 'react'
import { Image, Badge, Box, Flex, Highlight, Link, Text, HighlightProps } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ProtocolAdapter } from '@/api/__generated__'

interface AdapterTypeSummaryProps {
  adapter: ProtocolAdapter
  searchQuery?: string | null
}

const AdapterHighlight: FC<HighlightProps> = (props) => (
  <Highlight query={props.query} styles={{ rounded: 'full', bg: 'orange.100' }}>
    {props.children}
  </Highlight>
)

const AdapterTypeSummary: FC<AdapterTypeSummaryProps> = ({ adapter, searchQuery }) => {
  const { t } = useTranslation()

  return (
    <Flex m={0}>
      <Image boxSize="100px" objectFit="scale-down" src={adapter.logoUrl} aria-label={adapter.id} />
      <Box ml="3">
        <Text fontWeight="bold">
          <AdapterHighlight query={searchQuery || ''}>{adapter.name || ''}</AdapterHighlight>
          <Badge ml="1" colorScheme="green">
            {adapter.version}
          </Badge>
        </Text>
        <Text fontSize="sm">
          {t('protocolAdapter.overview.type')} {adapter.protocol}
        </Text>
        <Text fontSize="sm">
          {t('protocolAdapter.overview.author')} {adapter.author}
        </Text>
        <Text fontSize="sm">
          <AdapterHighlight query={searchQuery || ''}>{adapter.description || ''}</AdapterHighlight>
        </Text>
        <Box mt={2}>
          <Link href={adapter.url} isExternal>
            {t('protocolAdapter.overview.documentation')} <ExternalLinkIcon mx={1} mb={1} />
          </Link>
        </Box>
      </Box>
    </Flex>
  )
}

export default AdapterTypeSummary
