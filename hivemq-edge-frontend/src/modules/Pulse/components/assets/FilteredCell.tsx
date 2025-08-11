import type { FC } from 'react'
import { Skeleton, Highlight, Text } from '@chakra-ui/react'

interface FilteredCellProps {
  value: string
  isLoading?: boolean
  canGlobalFilter?: boolean
  globalFilter?: string
}

const FilteredCell: FC<FilteredCellProps> = ({
  value,
  isLoading = false,
  canGlobalFilter = false,
  globalFilter = '',
}) => {
  return (
    <Skeleton isLoaded={!isLoading}>
      <Text data-testid="cell-content">
        {canGlobalFilter && globalFilter ? (
          <Highlight
            data-testid="cell-content"
            query={globalFilter}
            styles={{ px: '2', py: '1', rounded: 'full', bg: 'yellow.50' }}
          >
            {value}
          </Highlight>
        ) : (
          value
        )}
      </Text>
    </Skeleton>
  )
}

export default FilteredCell
