import { Button, type ButtonProps } from '@chakra-ui/react'
import { FC } from 'react'

const ButtonCTA: FC<ButtonProps> = (props) => {
  const { children, ...rest } = props
  return (
    <Button {...rest} variant={'solid'} colorScheme={'yellow'}>
      {children}
    </Button>
  )
}

export default ButtonCTA
