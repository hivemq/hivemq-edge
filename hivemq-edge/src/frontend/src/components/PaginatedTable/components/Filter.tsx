import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Column } from '@tanstack/react-table'
import { CreatableSelect } from 'chakra-react-select'
import { Box } from '@chakra-ui/react'

export interface FilterProps<T>
  extends Pick<
    Column<T, unknown>,
    'id' | 'getFilterValue' | 'getFacetedUniqueValues' | 'getFacetedMinMaxValues' | 'setFilterValue'
  > {
  firstValue: unknown
}

export const Filter = <T,>({
  id,
  getFilterValue,
  getFacetedUniqueValues,
  // getFacetedMinMaxValues,
  setFilterValue,
  firstValue,
}: FilterProps<T>) => {
  const { t } = useTranslation()
  // const firstValue = table.getPreFilteredRowModel().flatRows[0]?.getValue(id)
  const columnFilterValue = getFilterValue()

  const sortedUniqueValues = useMemo(
    () => (typeof firstValue === 'number' ? [] : Array.from(getFacetedUniqueValues().keys()).sort()),
    [firstValue, getFacetedUniqueValues]
  )

  if (typeof firstValue === 'number') return null

  return (
    <Box w={'100%'} textTransform={'none'} fontWeight={'initial'}>
      <CreatableSelect
        size={'sm'}
        inputId={id}
        value={{ value: columnFilterValue, label: columnFilterValue }}
        onChange={(item) => setFilterValue(item?.value)}
        options={sortedUniqueValues
          .slice(0, 5000)
          .map((value: string) => ({ value: value, label: value, group: 'DDD' }))}
        placeholder={t('components:pagination.filter.placeholder', { size: getFacetedUniqueValues().size }) as string}
        isClearable={true}
        isMulti={false}
        components={{
          DropdownIndicator: null,
        }}
      />
    </Box>
  )
}
