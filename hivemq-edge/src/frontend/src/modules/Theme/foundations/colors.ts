import { theme as baseTheme } from '@chakra-ui/react'

const colors = {
  status: {
    error: baseTheme.colors.red,
    connected: baseTheme.colors.green,
    disconnected: baseTheme.colors.orange,
    stateless: baseTheme.colors.green,
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
  'hmq-Stone': {
    50: '#FAFAF9',
    100: '#F5F2EF',
    200: '#E7E5E4',
    300: '#D6D3D1',
    400: '#A8A29E',
    500: '#78716C',
    600: '#4C4747',
    700: '#44403C',
    800: '#292524',
    900: '#191614',
  },
  brandShort: {
    accent: '#3984BA',
    brand: '#003E6E',
  },
}

export default colors
