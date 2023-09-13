import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { formErrorAnatomy } from '@chakra-ui/anatomy'

const { defineMultiStyleConfig, definePartsStyle } = createMultiStyleConfigHelpers(formErrorAnatomy.keys)

const hivemq = definePartsStyle({
  text: {
    color: 'red.700',
  },
})

export const formErrorMessageTheme = defineMultiStyleConfig({
  baseStyle: hivemq,
})
