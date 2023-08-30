// Theming with Chakra UI is based on the Styled System Theme Specification
// Extend the theme to include custom colors, fonts, etc
import { defineStyleConfig, extendTheme } from '@chakra-ui/react'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/700.css'
import { statTheme } from './Stat'

const Button = defineStyleConfig({
  defaultProps: {
    colorScheme: 'brand',
    variant: 'outline',
  },
})

export const formStyles = {
  parts: ['container', 'requiredIndicator', 'helperText'],
  baseStyle: {
    container: {
      label: {
        fontWeight: 'bold',
      },
      h5: {
        fontSize: 'lg',
      },
      p: {
        margin: '4px',
        '.chakra-text': {
          fontSize: 'sm',
        },
      },
    },
  },
}

// export const theme = {
//     styles: {
//         global: {
//             'html, body': {
//                 color: 'gray.600',
//                 lineHeight: 'tall',
//             },
//             h5: {
//                 fontSize: "12px"
//             },
//             a: {
//                 color: 'teal.500',
//             },
//         },
//     },
// }

export const themeHiveMQ = extendTheme({
  fonts: {
    heading: `'Roboto', sans-serif`,
    body: `'Roboto', sans-serif`,
  },

  fontSizes: {
    lg: '16px',
    md: '14px',
    sm: '12px',
  },

  colors: {
    status: {
      connected: {
        500: '#38A169',
      },
      disconnected: {
        500: '#718096',
      },
      connecting: {
        500: '#CBD5E0',
      },
      disconnecting: {
        500: '#CBD5E0',
      },
      error: {
        500: '#E53E3E',
      },
    },
    // Based on 20-scale palette. One in two, omitting #fff
    brand: {
      50: '#ffffff',
      100: '#ccd8e2',
      200: '#99b2c5',
      300: '#668ba8',
      400: '#33658b',
      500: '#003e6e',
      600: '#003258',
      700: '#002542',
      800: '#00192c',
      900: '#000c16',
    },
    brandShort: {
      accent: '#3984BA',
      brand: '#003E6E',
    },
  },
  components: {
    Button,
    Stat: statTheme,
    Form: formStyles,
  },
})
