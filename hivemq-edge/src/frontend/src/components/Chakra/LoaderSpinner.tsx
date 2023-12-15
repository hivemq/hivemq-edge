import { Spinner, type SpinnerProps } from '@chakra-ui/react'
import { FC } from 'react'

const LoaderSpinner: FC<SpinnerProps> = (props) => {
  return (
    <Spinner
      data-testid={'loading-spinner'}
      thickness="4px"
      speed="0.65s"
      emptyColor="gray.200"
      color="brand.500"
      size="xl"
      {...props}
    />
  )
}

export default LoaderSpinner
