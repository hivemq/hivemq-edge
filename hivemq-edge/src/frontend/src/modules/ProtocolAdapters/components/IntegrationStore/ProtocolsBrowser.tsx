import { FC, useMemo } from 'react'
import { Box, Button, Card, CardBody, CardFooter, SimpleGrid } from '@chakra-ui/react'
import { ArrowForwardIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ProtocolAdapter } from '@/api/__generated__'
import WarningMessage from '@/components/WarningMessage.tsx'
import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import { ProtocolFacetType } from '../../types.ts'
import { applyFacets } from '../../utils/facets-utils.ts'
import AdapterTypeSummary from '../adapters/AdapterTypeSummary.tsx'

interface ProtocolsBrowserProps {
  items: ProtocolAdapter[]
  facet: ProtocolFacetType | undefined
  onCreate?: (adapterId: string | undefined) => void
}

const ProtocolsBrowser: FC<ProtocolsBrowserProps> = ({ items, facet, onCreate }) => {
  const { t } = useTranslation()
  const filteredAdapters = useMemo(() => {
    if (!facet) return items
    return items.filter(applyFacets(facet))
  }, [items, facet])

  if (!filteredAdapters.length)
    return (
      <Box width={'100%'}>
        <WarningMessage
          image={AdapterEmptyLogo}
          prompt={t('protocolAdapter.noFilterWarning.description')}
          alt={t('protocolAdapter.title')}
          mt={10}
        />
      </Box>
    )

  return (
    <SimpleGrid templateColumns={{ base: 'repeat(1, 1fr)', xl: 'repeat(2, 1fr)' }} spacing={4} gap={6}>
      {filteredAdapters?.map((e) => (
        <Card key={e.id} minW={'300px'}>
          <CardBody p={2}>
            <AdapterTypeSummary key={e.id} adapter={e} searchQuery={facet?.search} />
          </CardBody>
          <CardFooter p={2}>
            <Button
              data-testid={'protocol-create-adapter'}
              variant={'outline'}
              size={'sm'}
              rightIcon={<ArrowForwardIcon />}
              onClick={() => onCreate?.(e.id)}
            >
              {t('protocolAdapter.action.createInstance')}
            </Button>
          </CardFooter>
        </Card>
      ))}
    </SimpleGrid>
  )
}

export default ProtocolsBrowser
