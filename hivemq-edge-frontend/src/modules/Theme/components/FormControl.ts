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
    display: 'grid',
    gridTemplateColumns: 'max-content 1fr',
    alignItems: 'start',
    columnGap: '2',
    // Ensure children stack vertically in the second column
    '& > label': {
      gridColumn: 1,
      gridRow: 1,
      alignSelf: 'center',
      width: '4rem',
      textAlign: 'right',
      marginBottom: 0,
    },
    '& > *:not(label)': {
      gridColumn: 2,
    },
  },
})

export const formControlTheme = defineMultiStyleConfig({
  variants: { hivemq, horizontal },
})
