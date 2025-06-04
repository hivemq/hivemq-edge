import type { FC } from 'react'
import type { UseRadioProps } from '@chakra-ui/react'
import { Box, useRadio, Text } from '@chakra-ui/react'

interface RadioButtonProps extends UseRadioProps {
  children: React.ReactNode
}

export const RadioButton: FC<RadioButtonProps> = ({ children, ...rest }) => {
  const { getInputProps, getRadioProps } = useRadio(rest)

  return (
    <Box as="label">
      <input {...getInputProps()} />
      <Box
        {...getRadioProps()}
        cursor="pointer"
        borderWidth="1px"
        borderRadius="md"
        boxShadow="md"
        _checked={{
          bg: 'teal.600',
          color: 'white',
          borderColor: 'teal.600',
        }}
        _focus={{
          boxShadow: 'outline',
        }}
        px={5}
        py={1}
      >
        <Text>{children}</Text>
      </Box>
    </Box>
  )
}
