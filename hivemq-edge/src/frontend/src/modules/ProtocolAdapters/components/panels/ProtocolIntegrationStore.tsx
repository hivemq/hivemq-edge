import { FC, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Flex, Skeleton, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'

import config from '@/config'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'

import { ProtocolFacetType } from '../../types.ts'
import ProtocolsBrowser from '../IntegrationStore/ProtocolsBrowser.tsx'
import FacetSearch from '../IntegrationStore/FacetSearch.tsx'

const ProtocolIntegrationStore: FC = () => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetAdapterTypes()
  const navigate = useNavigate()
  const [facet, setFacet] = useState<ProtocolFacetType | undefined>(undefined)
  const isEmpty = useMemo(() => !data || !data.items || data.items?.length === 0, [data])

  const handleCreateInstance = (adapterId: string | undefined) => {
    navigate('/protocol-adapters/new', { state: { selectedAdapterId: adapterId } })
  }

  const handleOnSearch = (value: ProtocolFacetType) => {
    setFacet((old) => {
      const { search, filter } = old || {}
      return {
        search: value.search === undefined ? search : value.search,
        filter: value.filter === undefined ? filter : value.filter,
      }
    })
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
        mt={10}
      />
    )

  return (
    <Flex flexDirection={'column'} gap={4}>
      <Text> {t('protocolAdapter.protocols.description', { count: data.items ? data.items.length : 0 })} </Text>
      <Flex flexDirection={'row'} alignItems={'flex-start'} gap={6}>
        {config.features.PROTOCOL_ADAPTER_FACET && <FacetSearch facet={facet} onChange={handleOnSearch} />}
        <ProtocolsBrowser items={data.items as ProtocolAdapter[]} facet={facet} onCreate={handleCreateInstance} />
      </Flex>
    </Flex>
  )
}

export default ProtocolIntegrationStore
