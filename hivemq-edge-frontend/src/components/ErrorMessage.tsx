import type { FC } from 'react'
import type { AlertProps } from '@chakra-ui/react'
import { Alert, AlertDescription, AlertIcon, type AlertStatus, AlertTitle } from '@chakra-ui/react'

interface ErrorMessageProps extends AlertProps {
  type?: string | number
  message?: string
  status?: AlertStatus
}

const ErrorMessage: FC<ErrorMessageProps> = ({ type, message, status = 'error', ...rest }) => {
  return (
    <Alert status={status} {...rest}>
      <AlertIcon />
      <div>
        <AlertTitle>{type || ''}</AlertTitle>
        <AlertDescription>{message || ''}</AlertDescription>
      </div>
    </Alert>
  )
}

export default ErrorMessage
