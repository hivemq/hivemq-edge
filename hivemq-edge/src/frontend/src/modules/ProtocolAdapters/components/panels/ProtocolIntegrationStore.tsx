import { FC, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Button, Card, CardBody, CardFooter, Flex, SimpleGrid, Skeleton } from '@chakra-ui/react'
import { ArrowForwardIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'
import AdapterTypeSummary from '@/modules/ProtocolAdapters/components/adapters/AdapterTypeSummary.tsx'
import FacetSearch from '@/modules/ProtocolAdapters/components/IntegrationStore/FacetSearch.tsx'
import { ProtocolFacetType } from '@/modules/ProtocolAdapters/types.ts'

import config from '@/config'

const ProtocolIntegrationStore: FC = () => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetAdapterTypes()
  const navigate = useNavigate()
  const [facet, setFacet] = useState<ProtocolFacetType | undefined>(undefined)
  const isEmpty = useMemo(() => !data || !data.items || data.items?.length === 0, [data])

  const filteredAdapters = useMemo(() => {
    if (!facet) return data?.items
    return data?.items?.filter(
      (e) =>
        (facet.filter?.value === undefined || e.protocol === facet.filter?.value) &&
        (facet.search === undefined ||
          e.name?.toLowerCase().includes(facet.search) ||
          e.description?.toLowerCase().includes(facet.search))
    )
  }, [data, facet])

  const handleCreateInstance = (adapterId: string | undefined) => {
    navigate('/protocol-adapters/new', { state: { selectedAdapterId: adapterId } })
  }

  if (isError) {
    return (
      <Box mt={8}>
        <ErrorMessage type={error?.message} message={(error?.body as ProblemDetails)?.title} />
      </Box>
    )
  }

  if (isLoading) {
    return (
      <Flex flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
        <Skeleton width={250} height={100}></Skeleton>
      </Flex>
    )
  }

  if (isEmpty)
    return (
      <WarningMessage
        image={AdapterEmptyLogo}
        prompt={t('protocolAdapter.noDataWarning.description')}
        alt={t('protocolAdapter.title')}
      />
    )

  return (
    <Flex flexDirection={'column'} gap={4}>
      <div> {t('protocolAdapter.protocols.description', { count: data.items ? data.items.length : 0 })} </div>
      <Flex flexDirection={'row'} alignItems={'flex-start'} gap={6}>
        {config.features.PROTOCOL_ADAPTER_FACET && (
          <Flex>
            <FacetSearch facet={facet} setFacet={setFacet} />
          </Flex>
        )}
        <SimpleGrid mt={8} spacing={4} templateColumns={{ base: 'repeat(1, 1fr)', lg: 'repeat(2, 1fr)' }} gap={6}>
          {filteredAdapters?.map((e) => (
            <Card key={e.id}>
              <CardBody p={2}>
                <AdapterTypeSummary key={e.id} id={e.id} searchQuery={facet?.search} />
              </CardBody>
              <CardFooter p={2}>
                <Button
                  variant={'outline'}
                  size={'sm'}
                  rightIcon={<ArrowForwardIcon />}
                  onClick={() => handleCreateInstance(e.id)}
                >
                  {t('protocolAdapter.action.createInstance')}
                </Button>
              </CardFooter>
            </Card>
          ))}
        </SimpleGrid>
      </Flex>
    </Flex>
  )
}

export default ProtocolIntegrationStore
