import { FC, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Flex, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'

import config from '@/config'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'

import { AdapterNavigateState, ProtocolAdapterTabIndex, ProtocolFacetType } from '../../types.ts'
import ProtocolsBrowser from '../IntegrationStore/ProtocolsBrowser.tsx'
import FacetSearch from '../IntegrationStore/FacetSearch.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

const ProtocolIntegrationStore: FC = () => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetAdapterTypes()
  const navigate = useNavigate()
  const [facet, setFacet] = useState<ProtocolFacetType | undefined>(undefined)

  const safeData: ProtocolAdapter[] = data ? (data.items as ProtocolAdapter[]) : [mockProtocolAdapter]

  const handleCreateInstance = (adapterId: string | undefined) => {
    const adapterNavigateState: AdapterNavigateState = {
      protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
      protocolAdapterType: adapterId,
      // selectedActiveAdapter: { isNew: false, isOpen: false, adapterId: (selected?.data as Adapter).id },
    }
    navigate('/protocol-adapters/new', {
      state: adapterNavigateState,
    })
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
      <Box mt={'20%'} mx={'20%'} alignItems={'center'}>
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || (t('protocolAdapter.error.loading') as string)}
        />
      </Box>
    )
  }

  if (safeData.length === 0)
    return (
      <WarningMessage
        image={AdapterEmptyLogo}
        title={t('protocolAdapter.noTypeWarning.title') as string}
        prompt={t('protocolAdapter.noDataWarning.description')}
        alt={t('protocolAdapter.title')}
        mt={10}
      />
    )

  return (
    <Flex flexDirection={'column'} gap={4}>
      <Text>
        {isLoading
          ? t('protocolAdapter.loading.protocolAdapters')
          : t('protocolAdapter.protocols.description', { count: safeData.length })}{' '}
      </Text>
      <Flex flexDirection={'row'} alignItems={'flex-start'} gap={6}>
        {config.features.PROTOCOL_ADAPTER_FACET && (
          <FacetSearch items={safeData} facet={facet} onChange={handleOnSearch} isLoading={isLoading} />
        )}
        <ProtocolsBrowser items={safeData} facet={facet} onCreate={handleCreateInstance} isLoading={isLoading} />
      </Flex>
    </Flex>
  )
}

export default ProtocolIntegrationStore
