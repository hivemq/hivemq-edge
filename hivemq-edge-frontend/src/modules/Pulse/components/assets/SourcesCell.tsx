import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Text, VStack } from '@chakra-ui/react'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useListCombiners } from '@/api/hooks/useCombiners'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import { MAX_SOURCES_PER_ROW } from '@/modules/Pulse/utils/pagination-utils.ts'
import { EntityRenderer } from '@/modules/Mappings/combiner/EntityRenderer.tsx'

interface SourcesCellProps {
  mappingId: string
  isLoading?: boolean
}

const SourcesCell: FC<SourcesCellProps> = ({ mappingId }) => {
  const { t } = useTranslation()
  const { data, isLoading: isCombinersLoading } = useListCombiners()

  const sources = useMemo<EntityReference[] | undefined>(() => {
    if (!data?.items) return undefined

    const ownerCombiner = data.items.find((combiner) =>
      combiner.mappings?.items.some((mapping) => {
        return mapping.id === mappingId
      })
    )

    if (!ownerCombiner?.sources.items.length) return undefined

    return ownerCombiner.sources.items.filter((e) => e.type !== EntityType.PULSE_AGENT)
  }, [data?.items, mappingId])

  if (isCombinersLoading) return <LoaderSpinner />

  if (!sources)
    return (
      <Text data-testid="sources-error" whiteSpace="nowrap">
        {t('pulse.assets.listing.sources.norFound')}
      </Text>
    )

  const extraItems = Math.max(sources.length - MAX_SOURCES_PER_ROW, 0)

  return (
    <VStack data-testid="sources-container">
      {sources.slice(0, MAX_SOURCES_PER_ROW).map((reference) => (
        <EntityRenderer key={reference.id} reference={reference} />
      ))}
      {extraItems > 0 && <Text>{t('pulse.assets.listing.sources.more', { count: extraItems })}</Text>}
    </VStack>
  )
}

export default SourcesCell
