import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { statAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(statAnatomy.keys)

const hivemq = definePartsStyle({
  container: {
    borderRadius: 'lg',
    border: '4px solid',
    borderColor: 'blue.500',
    p: 1,
    backgroundColor: 'white',
    _dark: {
      backgroundColor: 'gray.700',
    },
  },
  helpText: {
    fontWeight: 'bold',
  },

  number: {
    fontStyle: 'italic',
    color: 'blue.500',
  },
})

export const statTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
