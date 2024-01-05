import { defineStyle, defineStyleConfig, SystemProps } from '@chakra-ui/react'

const spinner = defineStyle<SystemProps>({
  color: 'brand.500',
})

export const spinnerTheme = defineStyleConfig({
  defaultProps: {
    size: 'xl',
    variant: 'spinner',
  },
  variants: { spinner },
})
