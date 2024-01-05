import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { drawerAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(drawerAnatomy.keys)

const hivemq = definePartsStyle({
  dialog: {
    backgroundColor: 'gray.50',
    _dark: {
      backgroundColor: 'gray.800',
    },
  },
})

export const drawerTheme = defineMultiStyleConfig({
  defaultProps: {
    colorScheme: 'brand',
    variant: 'hivemq',
  },
  variants: { hivemq },
})
