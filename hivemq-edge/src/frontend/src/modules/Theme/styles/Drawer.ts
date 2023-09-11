import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { drawerAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(drawerAnatomy.keys)

const hivemq = definePartsStyle({
  dialog: {
    backgroundColor: 'gray.50',
  },
})

export const drawerTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
