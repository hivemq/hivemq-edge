import { FC } from 'react'
import { Table } from '@tanstack/react-table'
import {
  Box,
  ButtonGroup,
  Flex,
  FormControl,
  FormLabel,
  HStack,
  IconButton,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
  Select,
} from '@chakra-ui/react'
import { type IconButtonProps } from '@chakra-ui/react'
import { MdArrowLeft, MdArrowRight } from 'react-icons/md'
import { BiSkipNext, BiSkipPrevious } from 'react-icons/bi'

interface PaginationProps<T> {
  table: Table<T>
  pageSizes: number[]
  onFirstPage?: () => void
  onPreviousPage?: () => void
  onGoPage?: (n: number) => void
  onNextPage?: () => void
  onLastPage?: () => void
}

const PaginationButton: FC<IconButtonProps> = ({ children, ...rest }) => (
  <IconButton {...rest} size={'sm'} fontSize={'24px'}>
    {children}
  </IconButton>
)

const PaginationBar = <T,>({
  table,
  pageSizes,
  onFirstPage = () => table.setPageIndex(0),
  onPreviousPage = () => table.previousPage(),
  onGoPage = (n: number) => table.setPageIndex(n),
  onNextPage = () => table.nextPage(),
  onLastPage = () => table.setPageIndex(table.getPageCount() - 1),
}: PaginationProps<T>) => {
  return (
    <HStack gap={8} mt={4}>
      <Box>
        <ButtonGroup isAttached variant={'ghost'}>
          <PaginationButton
            icon={<BiSkipPrevious />}
            onClick={onFirstPage}
            aria-label={'sss'}
            isDisabled={!table.getCanPreviousPage()}
          ></PaginationButton>
          <PaginationButton
            icon={<MdArrowLeft />}
            onClick={onPreviousPage}
            aria-label={'sss'}
            isDisabled={!table.getCanPreviousPage()}
          >
            <MdArrowLeft />
          </PaginationButton>

          <PaginationButton onClick={onNextPage} aria-label={'sss'} isDisabled={!table.getCanNextPage()}>
            <MdArrowRight />
          </PaginationButton>
          <PaginationButton onClick={onLastPage} aria-label={'sss'} isDisabled={!table.getCanNextPage()}>
            <BiSkipNext />
          </PaginationButton>
        </ButtonGroup>
      </Box>

      <HStack>
        <FormControl display={'flex'} alignItems={'center'}>
          <FormLabel mb={0}>
            Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()} | Go to page
          </FormLabel>
          <NumberInput
            size="sm"
            maxWidth={'80px'}
            defaultValue={table.getState().pagination.pageIndex + 1}
            min={1}
            max={table.getPageCount()}
            onChange={(e) => {
              const page = Number(e) - 1
              onGoPage(page)
            }}
          >
            <NumberInputField />
            <NumberInputStepper>
              <NumberIncrementStepper />
              <NumberDecrementStepper />
            </NumberInputStepper>
          </NumberInput>
        </FormControl>
      </HStack>
      <Flex flex={1} justifyContent={'flex-end'}>
        <FormControl display={'flex'} alignItems={'baseline'} justifyContent={'flex-end'}>
          <FormLabel>Items per page</FormLabel>
          <Select
            maxWidth={'80px'}
            size={'sm'}
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
    </HStack>
  )
}

export default PaginationBar
