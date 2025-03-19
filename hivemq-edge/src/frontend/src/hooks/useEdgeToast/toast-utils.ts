import type { UseToastOptions } from '@chakra-ui/react'

export const BASE_TOAST_OPTION: UseToastOptions = {
  duration: 3000,
  isClosable: true,
}

export const DEFAULT_TOAST_OPTION: UseToastOptions = {
  ...BASE_TOAST_OPTION,
  status: 'success',
  position: 'top-right',
}
