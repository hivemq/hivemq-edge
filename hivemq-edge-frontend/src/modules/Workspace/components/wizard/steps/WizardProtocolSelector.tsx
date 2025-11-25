/**
 * Wizard Protocol Selector
 *
 * Step 1 of adapter wizard - select protocol type.
 * Uses standard drawer structure with proper header, body, and footer.
 */

import type { FC } from 'react'
import { useState } from 'react'
import {
  Box,
  DrawerHeader,
  DrawerBody,
  DrawerFooter,
  DrawerCloseButton,
  Heading,
  Text,
  Button,
  Grid,
  GridItem,
} from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import type { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import type { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import WarningMessage from '@/components/WarningMessage'
import AdapterEmptyLogo from '@/assets/app/adaptor-empty.svg'

import type { ProtocolFacetType } from '@/modules/ProtocolAdapters/types'
import FacetSearch from '@/modules/ProtocolAdapters/components/IntegrationStore/FacetSearch'
import ProtocolsBrowser from '@/modules/ProtocolAdapters/components/IntegrationStore/ProtocolsBrowser'
import { useWizardActions } from '@/modules/Workspace/hooks/useWizardStore'

interface WizardProtocolSelectorProps {
  onSelect: (protocolId: string | undefined) => void
}

/**
 * Step 1: Select protocol type
 * Shows a searchable list of available protocol adapters
 */
const WizardProtocolSelector: FC<WizardProtocolSelectorProps> = ({ onSelect }) => {
  const { t } = useTranslation()
  const { cancelWizard } = useWizardActions()
  const { data, isLoading, isError, error } = useGetAdapterTypes()
  const [facet, setFacet] = useState<ProtocolFacetType | undefined>(undefined)
  const [showSearch, setShowSearch] = useState(false)

  const handleOnSearch = (value: ProtocolFacetType) => {
    setFacet((old) => {
      const { search, filter } = old || {}
      return {
        search: value.search === undefined ? search : value.search,
        filter: value.filter === undefined ? filter : value.filter,
      }
    })
  }

  const safeData: ProtocolAdapter[] = data?.items || []

  return (
    <>
      <DrawerHeader borderBottomWidth="1px">
        <DrawerCloseButton onClick={cancelWizard} />
        <Heading size="md">{t('workspace.wizard.adapter.selectProtocol')}</Heading>
        <Text fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }} mt={1} fontWeight="normal">
          {t('workspace.wizard.adapter.selectProtocolDescription')}
        </Text>
      </DrawerHeader>

      <DrawerBody tabIndex={0}>
        {isLoading && <LoaderSpinner />}

        {isError && (
          <ErrorMessage
            type={error?.message}
            message={(error?.body as ProblemDetails)?.title || t('protocolAdapter.error.loading')}
          />
        )}

        {!isLoading && !isError && safeData.length === 0 && (
          <WarningMessage
            image={AdapterEmptyLogo}
            title={t('protocolAdapter.noTypeWarning.title')}
            prompt={t('protocolAdapter.noDataWarning.description')}
            alt={t('protocolAdapter.title')}
          />
        )}

        {!isLoading && !isError && safeData.length > 0 && (
          <>
            {showSearch ? (
              /* Two-column layout when search is visible */
              <Grid templateColumns="175px 1fr" gap={4} h="100%">
                <GridItem>
                  <FacetSearch items={safeData} facet={facet} onChange={handleOnSearch} />
                </GridItem>
                <GridItem overflowY="auto">
                  <ProtocolsBrowser
                    items={safeData}
                    facet={facet}
                    onCreate={onSelect}
                    isLoading={isLoading}
                    forceSingleColumn
                  />
                </GridItem>
              </Grid>
            ) : (
              /* Simple single column when search is hidden */
              <Box>
                <ProtocolsBrowser
                  items={safeData}
                  facet={facet}
                  onCreate={onSelect}
                  isLoading={isLoading}
                  forceSingleColumn
                />
              </Box>
            )}
          </>
        )}
      </DrawerBody>

      <DrawerFooter borderTopWidth="1px" justifyContent="center">
        <Button
          leftIcon={<SearchIcon />}
          size="sm"
          variant={showSearch ? 'solid' : 'outline'}
          onClick={() => setShowSearch(!showSearch)}
          data-testid={showSearch ? 'search-toggle-active' : 'search-toggle-inactive'}
        >
          {showSearch ? t('workspace.wizard.adapter.hideSearch') : t('workspace.wizard.adapter.showSearch')}
        </Button>
      </DrawerFooter>
    </>
  )
}

export default WizardProtocolSelector
