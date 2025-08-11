import type { BuiltInSortingFn } from '@tanstack/react-table'
import type { AriaAttributes, ReactNode } from 'react'

export interface FilterMetadata {
  filterOptions: {
    canCreate?: boolean
    filterType?: BuiltInSortingFn
    placeholder?: ReactNode
    noOptionsMessage?: (obj: { inputValue: string }) => ReactNode
    formatCreateLabel?: (inputValue: string) => ReactNode
    'aria-label'?: AriaAttributes['aria-label']
  }
}
