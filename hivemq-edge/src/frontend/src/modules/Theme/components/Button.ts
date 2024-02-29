import { defineStyle, defineStyleConfig, theme as baseTheme, SystemStyleObject } from '@chakra-ui/react'

const primary = defineStyle(
  (props): SystemStyleObject => ({
    ...baseTheme.components.Button.variants?.solid({ ...props, colorScheme: 'yellow' }),
  })
)

const danger = defineStyle(
  (props): SystemStyleObject => ({
    ...baseTheme.components.Button.variants?.outline({ ...props, colorScheme: 'red' }),
  })
)

export const buttonTheme = defineStyleConfig({
  defaultProps: {
    colorScheme: 'brand',
    variant: 'outline',
  },
  variants: { ...baseTheme.components.Button.variants, primary, danger },
})
