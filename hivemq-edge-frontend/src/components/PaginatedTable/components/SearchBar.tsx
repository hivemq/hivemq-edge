import type { FC } from 'react'
import type { ColumnFiltersState, Updater } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import {
  Button,
  FormControl,
  HStack,
  Icon,
  IconButton,
  Input,
  InputGroup,
  InputLeftAddon,
  InputRightElement,
} from '@chakra-ui/react'
import { CloseIcon, SearchIcon } from '@chakra-ui/icons'

interface SearchBarProps {
  columnFilters: ColumnFiltersState
  globalFilter: string
  enableGlobalFilter?: boolean
  enableColumnFilters?: boolean
  setGlobalFilter?: (updater: Updater<string>) => void
  resetColumnFilters?: (defaultState?: boolean) => void
}

const SearchBar: FC<SearchBarProps> = ({
  setGlobalFilter,
  resetColumnFilters,
  globalFilter,
  columnFilters,
  enableGlobalFilter = false,
  enableColumnFilters = false,
}) => {
  const { t } = useTranslation('components')

  return (
    <HStack role="toolbar" aria-label={t('SearchBar.aria-label')} gap={8} flex={1} m={1}>
      {enableGlobalFilter && (
        <FormControl display="flex" maxW={250}>
          <InputGroup size="sm">
            <InputLeftAddon aria-hidden={true}>
              <Icon as={SearchIcon} boxSize="3" />
            </InputLeftAddon>
            <Input
              placeholder="Search for ..."
              onChange={(evt) => setGlobalFilter?.(evt.target.value)}
              value={globalFilter}
              pr="1.8rem"
            />
            <InputRightElement width="2rem">
              {globalFilter && (
                <IconButton
                  icon={<Icon as={CloseIcon} />}
                  variant="ghost"
                  size="xs"
                  aria-label={t('pagination.filter.clear')}
                  onClick={() => setGlobalFilter?.('')}
                />
              )}
            </InputRightElement>
          </InputGroup>
        </FormControl>
      )}
      {enableColumnFilters && (
        <Button
          isDisabled={!globalFilter && columnFilters.length === 0}
          size="sm"
          onClick={() => {
            resetColumnFilters?.()
            setGlobalFilter?.('')
          }}
        >
          {t('pagination.filter.clearAll')}
        </Button>
      )}
    </HStack>
  )
}

export default SearchBar
