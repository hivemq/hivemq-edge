import { UseToastOptions } from '@chakra-ui/react'

// TODO[NVL] Should be exported
export type ToastStatus = 'default' | 'success' | 'error' | 'warning' | 'info' | 'loading'

export const DEFAULT_TOAST_OPTION: UseToastOptions = {
  status: 'success',
  duration: 3000,
  isClosable: true,
  position: 'top-right',
}
