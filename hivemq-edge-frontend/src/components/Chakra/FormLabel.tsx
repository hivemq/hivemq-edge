import type { FormLabelProps } from '@chakra-ui/react'
import { HStack, FormLabel as CuiFormLabel } from '@chakra-ui/react'
import type { FC } from 'react'

interface FormLabelExtProps extends FormLabelProps {
  rightAddon?: React.ReactNode
}

const FormLabel: FC<FormLabelExtProps> = ({ rightAddon, children, ...labelProps }) => {
  return (
    <HStack mb={2} gap={3}>
      <CuiFormLabel {...labelProps} mb={0} marginInlineEnd={0}>
        {children}
      </CuiFormLabel>
      {rightAddon}
    </HStack>
  )
}

export default FormLabel
