import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { formErrorAnatomy } from '@chakra-ui/anatomy'

const { defineMultiStyleConfig, definePartsStyle } = createMultiStyleConfigHelpers(formErrorAnatomy.keys)

const hivemq = definePartsStyle({
  text: {
    // Fix "color-contrast" (WCAG)
    color: 'red.600',
    _dark: {
      color: 'red.300',
    },
  },
})

export const formErrorMessageTheme = defineMultiStyleConfig({
  baseStyle: hivemq,
})
