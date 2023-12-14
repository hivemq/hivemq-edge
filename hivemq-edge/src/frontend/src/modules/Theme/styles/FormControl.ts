import { formAnatomy } from '@chakra-ui/anatomy'
import { createMultiStyleConfigHelpers } from '@chakra-ui/react'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(formAnatomy.keys)

const hivemq = definePartsStyle({
  container: {
    backgroundColor: 'white',
    padding: 4,
    borderRadius: 6,
    borderWidth: 1,

    h5: {
      fontSize: 'lg',
    },
  },
})

export const formControlTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
