import { createMultiStyleConfigHelpers } from '@chakra-ui/react'
import { formAnatomy } from '@chakra-ui/anatomy'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(formAnatomy.keys)

const hivemq = definePartsStyle({
  container: {
    backgroundColor: 'white',
    _dark: {
      backgroundColor: 'gray.700',
    },
    padding: 4,
    borderRadius: 6,
    borderWidth: 1,

    h5: {
      fontSize: 'lg',
    },
  },
})

const horizontal = definePartsStyle({
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 3,
    '> label': {
      flex: 1,
      marginBottom: 0,
      marginInlineEnd: 0,
    },
    '> label + *': {
      flex: 2,
    },
  },
})

export const formControlTheme = defineMultiStyleConfig({
  variants: { hivemq, horizontal },
})
