import { FC } from 'react'
import { Image, Badge, Box, Flex, Highlight, Link, Text, HighlightProps } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'

interface AdapterTypeSummaryProps {
  id?: string
  searchQuery?: string
}

const AdapterHighlight: FC<HighlightProps> = (props) => (
  <Highlight query={props.query} styles={{ pl: 2, py: '1', rounded: 'full', bg: 'gray.100' }}>
    {props.children}
  </Highlight>
)

const AdapterTypeSummary: FC<AdapterTypeSummaryProps> = ({ id, searchQuery }) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()
  const selectedType = data?.items?.find((e) => e.id === id)

  if (!selectedType) return null
  return (
    <Flex m={0}>
      <Image boxSize="100px" objectFit="scale-down" src={selectedType.logoUrl} aria-label={selectedType.id} />
      <Box ml="3">
        <Text fontWeight="bold">
          <AdapterHighlight query={searchQuery || ''}>{selectedType.name || ''}</AdapterHighlight>
          <Badge ml="1" colorScheme="green">
            {selectedType.version}
          </Badge>
        </Text>
        <Text fontSize="sm">
          {t('protocolAdapter.overview.type')} {selectedType.protocol}
        </Text>
        <Text fontSize="sm">
          {t('protocolAdapter.overview.author')} {selectedType.author}
        </Text>
        <Text fontSize="sm">
          <AdapterHighlight query={searchQuery || ''}>{selectedType.description || ''}</AdapterHighlight>
        </Text>
        <Box mt={2}>
          <Link href={selectedType.url} isExternal>
            {t('protocolAdapter.overview.documentation')} <ExternalLinkIcon mx={1} mb={1} />
          </Link>
        </Box>
      </Box>
    </Flex>
  )
}

export default AdapterTypeSummary
