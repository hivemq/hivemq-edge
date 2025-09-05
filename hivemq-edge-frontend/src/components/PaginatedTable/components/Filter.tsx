import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { Column } from '@tanstack/react-table'
import { CreatableSelect, Select } from 'chakra-react-select'
import { Box } from '@chakra-ui/react'
import { DateTime } from 'luxon'

import DateTimeRangeSelector from '@/components/DateTime/DateTimeRangeSelector.tsx'
import type { FilterMetadata } from '@/components/PaginatedTable/types.ts'

export interface FilterProps<T>
  extends Pick<
    Column<T, unknown>,
    'id' | 'getFilterValue' | 'getFacetedUniqueValues' | 'getFacetedMinMaxValues' | 'setFilterValue' | 'columnDef'
  > {
  firstValue: unknown
}

export const Filter = <T,>({
  id,
  getFilterValue,
  getFacetedUniqueValues,
  getFacetedMinMaxValues,
  setFilterValue,
  firstValue,
  columnDef,
}: FilterProps<T>) => {
  const { t } = useTranslation()

  const filterOptions = (columnDef.meta as FilterMetadata)?.filterOptions

  const canCreateOptions = filterOptions?.canCreate ?? true
  const filterType = filterOptions?.filterType ?? 'text'
  const placeholder =
    filterOptions?.placeholder ?? t('components:pagination.filter.placeholder', { size: getFacetedUniqueValues().size })
  const noOptionsMessage = (obj: { inputValue: string }) =>
    filterOptions?.noOptionsMessage?.(obj) ?? t('components:pagination.filter.noOptions')
  const formatCreateLabel = (inputValue: string) =>
    filterOptions?.formatCreateLabel?.(inputValue) ?? t('components:pagination.filter.create', { topic: inputValue })
  const ariaLabel = filterOptions?.['aria-label'] ?? t('components:pagination.filter.label')

  const columnValue = getFilterValue()
  const facetedUniqueValues = getFacetedUniqueValues()
  const sortedUniqueValues = useMemo(() => {
    if (typeof firstValue === 'number') return []
    const keys = Array.from(facetedUniqueValues.keys())
    const isNotString = keys.some((e) => Array.isArray(e))

    if (isNotString) {
      // TODO[35496] Assumed to be arrays; will need fine-tuning
      return Array.from(
        new Set(
          keys
            .flat()
            // TODO This is a hack. Need to pass a key extractor to the filter
            .map<string>((e) => e?.id)
        )
      ).sort((a, b) => a.localeCompare(b))
    }

    return (keys satisfies string[]).sort((a, b) => a.localeCompare(b))
  }, [facetedUniqueValues, firstValue])

  if (typeof firstValue === 'number' && filterType === 'datetime') {
    // TODO[NVL] This is a weird typing, as the function doesn't match the type
    const [a, b] = getFacetedMinMaxValues() || [undefined, undefined]
    const min = Number(a)
    const max = Number(b)

    return (
      <Box w="100%" textTransform="none" fontWeight="initial" data-testid="filter-wrapper">
        <DateTimeRangeSelector
          id={id}
          min={DateTime.fromMillis(min)}
          max={DateTime.fromMillis(max)}
          setFilterValue={(v) => {
            if (v) setFilterValue([v[0], v[1]])
            else setFilterValue(undefined)
          }}
        />
      </Box>
    )
  }

  // we are not supporting numbers yet
  if (typeof firstValue === 'number') return null

  const SelectComponent = canCreateOptions ? CreatableSelect : Select

  return (
    <Box w="100%" textTransform="none" fontWeight="initial" data-testid="filter-wrapper">
      <SelectComponent
        size="sm"
        inputId={id}
        instanceId={id}
        menuPortalTarget={document.body}
        value={columnValue ? { value: columnValue, label: columnValue } : null}
        onChange={(item) => setFilterValue(item?.value)}
        // TODO[35494] The label/renderer for the options need customisation per column
        options={sortedUniqueValues.map((value: string) => ({ value: value, label: value }))}
        placeholder={placeholder}
        noOptionsMessage={noOptionsMessage}
        formatCreateLabel={formatCreateLabel}
        aria-label={ariaLabel}
        isClearable={true}
        isMulti={false}
        components={{
          DropdownIndicator: null,
          ...filterOptions?.components,
        }}
        chakraStyles={{
          menuList: (provided) => ({
            ...provided,
            width: 'fit-content',
          }),
        }}
        styles={{
          menuPortal: (provided) => ({ ...provided, zIndex: 'var(--chakra-zIndices-popover)' }),
        }}
      />
    </Box>
  )
}
