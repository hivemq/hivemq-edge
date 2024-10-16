import { FC } from 'react'
import { Alert, AlertDescription, AlertIcon, type AlertStatus, AlertTitle } from '@chakra-ui/react'

interface ErrorMessageProps {
  type?: string | number
  message?: string
  status?: AlertStatus
}

const ErrorMessage: FC<ErrorMessageProps> = ({ type, message, status = 'error' }) => {
  return (
    <Alert status={status}>
      <AlertIcon />
      <div>
        <AlertTitle>{type || ''}</AlertTitle>
        <AlertDescription>{message || ''}</AlertDescription>
      </div>
    </Alert>
  )
}

export default ErrorMessage
