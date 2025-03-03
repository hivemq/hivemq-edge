import { useCallback } from 'react'
import type { CustomValidator, RJSFSchema } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'

import type { Combiner, DomainTagList, TopicFilterList } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'

import type { CombinerContext } from '@/modules/Mappings/types'

// TODO[NVL] Context is not part of the customValidator props; need to get a better construction of props
export const useValidateCombiner = (queries: UseQueryResult<DomainTagList | TopicFilterList, Error>[]) => {
  const { data: adapterInfo } = useGetAdapterTypes()
  const { data: adapters } = useListProtocolAdapters()

  /**
   * Verify that a connected entity is eligible for data combining:
   * - ADAPTER: must have the `COMBINE` capability
   * - BRIDGE: no condition
   *     TODO[NVL] Should we ban bridges that have no proper topic filters
   * - EDGE: mandatory inclusion to the sources
   */
  const validateSourceCapability = useCallback<CustomValidator<Combiner, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      formData?.sources.items.forEach((entity, index) => {
        if (entity.type === EntityType.ADAPTER) {
          const adapter = adapters?.find((e) => e.id === entity.id)
          const protocolAdapter = adapter
            ? adapterInfo?.items.find((protocol) => protocol.id === adapter.type)
            : undefined
          if (!adapter || !protocolAdapter)
            errors.sources?.items?.[index]?.addError('This is not a valid reference to a Workspace entity')
          else {
            if (protocolAdapter.capabilities && !protocolAdapter.capabilities.includes('COMBINE')) {
              errors.sources?.items?.[index]?.addError(
                'The adapter does not support data combining and cannot be used as a source'
              )
            }
          }
        }
      })

      const hasEdge = formData?.sources.items?.filter((e) => e.type === EntityType.EDGE_BROKER)
      if (!hasEdge || hasEdge.length !== 1) {
        errors.sources?.items?.addError("The Edge broker must be connected to the combiner's sources")
      }

      return errors
    },
    [adapterInfo?.items, adapters]
  )

  const validateCombiner = useCallback<CustomValidator<Combiner, RJSFSchema, CombinerContext>>(
    (formData, errors) => {
      validateSourceCapability(formData, errors)

      formData?.mappings?.items?.forEach((entity, index) => {
        if (!errors.mappings?.items?.[index]) return

        console.log('XXX', queries, entity)
        // TODO
      })

      return errors
    },
    [queries, validateSourceCapability]
  )

  return validateCombiner
}
