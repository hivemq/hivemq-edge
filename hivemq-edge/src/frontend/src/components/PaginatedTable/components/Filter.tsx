import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Column } from '@tanstack/react-table'
// import { RangeSlider, RangeSliderFilledTrack, RangeSliderThumb, RangeSliderTrack } from '@chakra-ui/react'

import { DebouncedInput } from './DebouncedInput.tsx'

export interface FilterProps<T>
  extends Pick<
    Column<T, unknown>,
    'id' | 'getFilterValue' | 'getFacetedUniqueValues' | 'getFacetedMinMaxValues' | 'setFilterValue'
  > {
  firstValue: unknown
}

// /**/export const Filter = <T,>({ column, table }: { column: Column<T, unknown>; table: Table<T> }) => {
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

  // const FilterMulti = (
  //   <div className="flex space-x-2">
  //     <DebouncedInput
  //       type="number"
  //       min={Number(getFacetedMinMaxValues()?.[0] ?? '')}
  //       max={Number(getFacetedMinMaxValues()?.[1] ?? '')}
  //       value={(columnFilterValue as [number, number])?.[0] ?? ''}
  //       onChange={(value) => setFilterValue((old: [number, number]) => [value, old?.[1]])}
  //       placeholder={`Min ${getFacetedMinMaxValues()?.[0] ? `(${getFacetedMinMaxValues()?.[0]})` : ''}`}
  //       className="w-24 border shadow rounded"
  //     />
  //     <DebouncedInput
  //       type="number"
  //       min={Number(getFacetedMinMaxValues()?.[0] ?? '')}
  //       max={Number(getFacetedMinMaxValues()?.[1] ?? '')}
  //       value={(columnFilterValue as [number, number])?.[1] ?? ''}
  //       onChange={(value) => setFilterValue((old: [number, number]) => [old?.[0], value])}
  //       placeholder={`Max ${getFacetedMinMaxValues()?.[1] ? `(${getFacetedMinMaxValues()?.[1]})` : ''}`}
  //       className="w-24 border shadow rounded"
  //     />
  //   </div>
  // )
  //
  // const FilterRangeSlider = (
  //   <RangeSlider
  //     onChangeEnd={(a) => console.log('XXXXX', a)}
  //     aria-label={['min', 'max']}
  //     defaultValue={[Number(getFacetedMinMaxValues()?.[0] ?? ''), Number(getFacetedMinMaxValues()?.[1] ?? '')]}
  //     min={Number(getFacetedMinMaxValues()?.[0] ?? '')}
  //     max={Number(getFacetedMinMaxValues()?.[1] ?? '')}
  //   >
  //     <RangeSliderTrack>
  //       <RangeSliderFilledTrack />
  //     </RangeSliderTrack>
  //     <RangeSliderThumb index={0} />
  //     <RangeSliderThumb index={1} />
  //   </RangeSlider>
  // )

  if (typeof firstValue === 'number') return null

  return (
    <>
      <datalist id={id + '-list'}>
        {sortedUniqueValues.slice(0, 5000).map((value: any) => (
          <option value={value} key={value} />
        ))}
      </datalist>
      <DebouncedInput
        type="text"
        value={(columnFilterValue ?? '') as string}
        onChange={(value) => setFilterValue(value)}
        placeholder={t('components:pagination.filter.placeholder', { size: getFacetedUniqueValues().size }) as string}
        list={id + '-list'}
      />
    </>
  )
}
