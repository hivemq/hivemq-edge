import type { FC } from 'react'
import type { Table } from '@tanstack/react-table'
import { useTranslation } from 'react-i18next'
import {
  Box,
  ButtonGroup,
  Flex,
  FormControl,
  FormLabel,
  HStack,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
  Select,
  Text,
  type IconButtonProps,
} from '@chakra-ui/react'
import { LuSkipBack, LuSkipForward, LuStepBack, LuStepForward } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

interface PaginationProps<T> {
  table: Table<T>
  pageSizes: number[]
  options?: {
    enablePaginationSizes?: boolean
    enablePaginationGoTo?: boolean
  }
}

const PaginationButton: FC<IconButtonProps> = (props) => (
  <IconButton icon={<LuSkipBack />} {...props} size="sm" fontSize="18px" />
)

const PaginationBar = <T,>({ table, pageSizes, options }: PaginationProps<T>) => {
  const { t } = useTranslation()
  return (
    <HStack as="nav" aria-label={t('components:pagination.ariaLabel')} gap={8} mt={4}>
      <ButtonGroup isAttached variant="ghost">
        <PaginationButton
          icon={<LuSkipBack />}
          onClick={() => table.setPageIndex(0)}
          aria-label={t('components:pagination.goFirstPage')}
          isDisabled={!table.getCanPreviousPage()}
        />
        <PaginationButton
          icon={<LuStepBack />}
          onClick={() => table.previousPage()}
          aria-label={t('components:pagination.goPreviousPage')}
          isDisabled={!table.getCanPreviousPage()}
        />
        <PaginationButton
          icon={<LuStepForward />}
          onClick={() => table.nextPage()}
          aria-label={t('components:pagination.goNextPage')}
          isDisabled={!table.getCanNextPage()}
        />
        <PaginationButton
          icon={<LuSkipForward />}
          onClick={() => table.setPageIndex(table.getPageCount() - 1)}
          aria-label={t('components:pagination.goLastPage')}
          isDisabled={!table.getCanNextPage()}
        />
      </ButtonGroup>

      <Box role="group">
        <Text fontSize="md" whiteSpace="nowrap">
          {t('components:pagination.pageOf', {
            page: table.getState().pagination.pageIndex + 1,
            max: table.getPageCount(),
          })}
        </Text>
      </Box>

      {options?.enablePaginationGoTo && (
        <FormControl display="flex" alignItems="center" w="inherit">
          <FormLabel mb={0}>{t('components:pagination.goPage')}</FormLabel>
          <NumberInput
            size="sm"
            maxWidth="80px"
            defaultValue={table.getState().pagination.pageIndex + 1}
            min={1}
            max={table.getPageCount()}
            onChange={(e) => table.setPageIndex(Number(e) - 1)}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
        </FormControl>
      )}

      {options?.enablePaginationSizes && (
        <Flex flex={1}>
          <FormControl display="flex" alignItems="baseline" justifyContent="flex-end">
            <FormLabel> {t('components:pagination.perPage')}</FormLabel>
            <Select
              maxWidth="80px"
              size="sm"
              value={table.getState().pagination.pageSize}
              onChange={(e) => {
                table.setPageSize(Number(e.target.value))
              }}
            >
              {pageSizes.map((pageSize) => (
                <option key={pageSize} value={pageSize}>
                  {pageSize}
                </option>
              ))}
            </Select>
          </FormControl>
        </Flex>
      )}
    </HStack>
  )
}

export default PaginationBar
