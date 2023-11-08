import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Column } from '@tanstack/react-table'
import { CreatableSelect } from 'chakra-react-select'
import { Box, VStack } from '@chakra-ui/react'
import { DateTime } from 'luxon'

import DateTimeRangeSelector from '@/components/DateTime/DateTimeRangeSelector.tsx'

export interface FilterProps<T>
  extends Pick<
    Column<T, unknown>,
    'id' | 'getFilterValue' | 'getFacetedUniqueValues' | 'getFacetedMinMaxValues' | 'setFilterValue'
  > {
  firstValue: unknown
}

export const Filter = <T,>({
  id,
  // getFilterValue,
  getFacetedUniqueValues,
  // getFacetedMinMaxValues,
  setFilterValue,
  firstValue,
}: FilterProps<T>) => {
  const { t } = useTranslation()

  const facetedUniqueValues = getFacetedUniqueValues()
  const sortedUniqueValues = useMemo(
    () => (typeof firstValue === 'number' ? [] : Array.from(facetedUniqueValues.keys()).sort()),
    [facetedUniqueValues, firstValue]
  )

  if (firstValue instanceof DateTime)
    return (
      <VStack width={'100%'} textTransform={'none'} fontWeight={'initial'}>
        <Box w={'100%'}>
          <DateTimeRangeSelector />
        </Box>
        <Box w={'100%'}>
          <DateTimeRangeSelector />
        </Box>
      </VStack>
    )

  // we are not supporting numbers yet
  if (typeof firstValue === 'number') return null

  return (
    <Box w={'100%'} textTransform={'none'} fontWeight={'initial'}>
      <CreatableSelect
        size={'sm'}
        inputId={id}
        menuPortalTarget={document.body}
        // value={{ value: columnFilterValue, label: columnFilterValue }}
        onChange={(item) => setFilterValue(item?.value)}
        options={sortedUniqueValues.map((value: string) => ({ value: value, label: value, group: 'DDD' }))}
        placeholder={t('components:pagination.filter.placeholder', { size: getFacetedUniqueValues().size }) as string}
        noOptionsMessage={() => t('components:pagination.filter.noOptions')}
        formatCreateLabel={(e) => t('components:pagination.filter.create', { topic: e })}
        aria-label={t('components:pagination.filter.label') as string}
        isClearable={true}
        isMulti={false}
        components={{
          DropdownIndicator: null,
        }}
      />
    </Box>
  )
}
