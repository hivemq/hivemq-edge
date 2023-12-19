// Theming with Chakra UI is based on the Styled System Theme Specification
// Extend the theme to include custom colors, fonts, etc
import { extendTheme } from '@chakra-ui/react'

import components from './components'
import colors from './foundations/colors.ts'

const themeHiveMQ = extendTheme({
  fonts: {
    heading: `'Roboto', sans-serif`,
    body: `'Roboto', sans-serif`,
  },

  // fontSizes: {
  //   lg: '16px',
  //   md: '14px',
  //   sm: '12px',
  // },

  colors: colors,
  components: components,
})

export default themeHiveMQ
