import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { statAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(statAnatomy.keys)

const hivemq = definePartsStyle({
  container: {
    borderRadius: 'lg',
    border: '2px solid',
    borderColor: 'blue.500',
    p: 1,
  },
  helpText: {
    fontWeight: 'bold',
    color: 'blue.500',
  },
  label: {
    color: 'blue.500',
  },
  number: {
    fontStyle: 'italic',
    color: 'blue.500',
  },
})

export const statTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
