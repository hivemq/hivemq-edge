import type { FC } from 'react'
import type { AlertProps } from '@chakra-ui/react'
import { VStack } from '@chakra-ui/react'
import { Alert, AlertDescription, AlertIcon, type AlertStatus, AlertTitle } from '@chakra-ui/react'

interface ErrorMessageProps extends AlertProps {
  type?: string | number
  message?: string
  status?: AlertStatus
  stack?: Error
}

const ErrorMessage: FC<ErrorMessageProps> = ({ type, message, stack, status = 'error', ...rest }) => {
  return (
    <Alert status={status} {...rest}>
      <AlertIcon />
      <VStack alignItems="flex-start">
        <AlertTitle>{type || ''}</AlertTitle>
        <AlertDescription>{message || ''}</AlertDescription>
        {stack && <AlertDescription>{stack?.message}</AlertDescription>}
      </VStack>
    </Alert>
  )
}

export default ErrorMessage
