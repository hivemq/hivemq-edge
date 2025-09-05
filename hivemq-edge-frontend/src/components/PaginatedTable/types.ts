import type { BuiltInSortingFn } from '@tanstack/react-table'
import type { AriaAttributes, ReactNode } from 'react'
import type { SelectComponentsConfig, GroupBase } from 'chakra-react-select'

export interface FilterMetadata {
  filterOptions: {
    canCreate?: boolean
    filterType?: BuiltInSortingFn
    placeholder?: ReactNode
    noOptionsMessage?: (obj: { inputValue: string }) => ReactNode
    formatCreateLabel?: (inputValue: string) => ReactNode
    'aria-label'?: AriaAttributes['aria-label']
    components?: SelectComponentsConfig<unknown, boolean, GroupBase<unknown>>
  }
}
