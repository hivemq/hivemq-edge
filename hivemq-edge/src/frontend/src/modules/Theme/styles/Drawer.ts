import { drawerAnatomy } from '@chakra-ui/anatomy'
import { createMultiStyleConfigHelpers } from '@chakra-ui/react'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(drawerAnatomy.keys)

const hivemq = definePartsStyle({
  dialog: {
    backgroundColor: 'gray.50',
  },
})

export const drawerTheme = defineMultiStyleConfig({
  variants: { hivemq },
})
