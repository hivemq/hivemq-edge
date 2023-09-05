import { ProtocolFacetType } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapter } from '@/api/__generated__'

export const applyFacets = (facet: ProtocolFacetType): ((e: ProtocolAdapter) => boolean) => {
  return (adapter) => {
    const isFilterUnset = !facet.filter?.value
    const isCategoryMatching = Boolean(
      (facet.filter?.key === 'category' && adapter.category?.name === facet.filter?.value) ||
        (facet.filter?.key === 'tags' && adapter.tags?.includes(facet.filter?.value))
    )
    const isSearchUnset = !facet.search
    const isTextMatching = Boolean(
      facet.search &&
        (adapter.name?.toLowerCase().includes(facet.search.toLowerCase()) ||
          adapter.description?.toLowerCase().includes(facet.search.toLowerCase()))
    )

    return (isFilterUnset || isCategoryMatching) && (isSearchUnset || isTextMatching)
  }
}
