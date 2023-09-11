import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { formAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(formAnatomy.keys)

const hivemq = definePartsStyle({
  container: {
    backgroundColor: 'white',
    padding: 4,
    borderRadius: 6,
    borderWidth: 1,
  },
})

export const formControlTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
