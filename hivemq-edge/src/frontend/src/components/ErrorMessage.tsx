import { Alert, AlertDescription, AlertIcon, AlertTitle } from '@chakra-ui/react'
import { FC } from 'react'

interface ErrorMessageProps {
  type?: string | number
  message?: string
}

const ErrorMessage: FC<ErrorMessageProps> = ({ type, message }) => {
  return (
    <Alert status="error">
      <AlertIcon />
      <div>
        <AlertTitle>{type || ''}</AlertTitle>
        <AlertDescription>{message || ''}</AlertDescription>
      </div>
    </Alert>
  )
}

export default ErrorMessage
