import { FC } from 'react'
import { Spinner, type SpinnerProps } from '@chakra-ui/react'

const LoaderSpinner: FC<SpinnerProps> = (props) => {
  return <Spinner data-testid={'loading-spinner'} thickness="4px" speed="0.65s" emptyColor="gray.200" {...props} />
}

export default LoaderSpinner
