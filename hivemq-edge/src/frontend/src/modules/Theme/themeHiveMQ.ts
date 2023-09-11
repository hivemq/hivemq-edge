// Theming with Chakra UI is based on the Styled System Theme Specification
// Extend the theme to include custom colors, fonts, etc
import { defineStyleConfig, extendTheme, theme as baseTheme } from '@chakra-ui/react'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/700.css'

import { statTheme } from './styles/Stat'
import { drawerTheme } from './styles/Drawer.ts'
import { formControlTheme } from './styles/FormControl.ts'

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
        fontWeight: 'medium',
      },
      h5: {
        fontSize: 'lg',
      },
      // p: {
      //   margin: '4px',
      //   '.chakra-text': {
      //     fontSize: 'sm',
      //   },
      // },
    },
  },
}

export const TestStyles = {
  parts: ['container', 'requiredIndicator', 'helperText'],
  baseStyle: {
    container: {
      backgroundColor: 'red',
      label: {
        fontWeight: 'medium',
      },
      h5: {
        fontSize: 'lg',
      },
      // p: {
      //   margin: '4px',
      //   '.chakra-text': {
      //     fontSize: 'sm',
      //   },
      // },
    },
  },
}

export const themeHiveMQ = extendTheme({
  fonts: {
    heading: `'Roboto', sans-serif`,
    body: `'Roboto', sans-serif`,
  },

  // fontSizes: {
  //   lg: '16px',
  //   md: '14px',
  //   sm: '12px',
  // },

  colors: {
    status: {
      connected: baseTheme.colors.green,
      disconnected: baseTheme.colors.gray,
      connecting: baseTheme.colors.orange,
      disconnecting: baseTheme.colors.orange,
      error: baseTheme.colors.red,
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
    Drawer: drawerTheme,
    Form: formControlTheme,
  },
})
