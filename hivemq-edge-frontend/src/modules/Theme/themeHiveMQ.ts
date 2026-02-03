// Theming with Chakra UI is based on the Styled System Theme Specification
// Extend the theme to include custom colors, fonts, etc
import { extendTheme } from '@chakra-ui/react'

import components from './components'
import colors from './foundations/colors.ts'
import semanticTokens from '@/modules/Theme/foundations/semanticTokens.ts'
import { treeView } from '@/modules/Theme/globals/treeview.ts'
import { reactFlow } from '@/modules/Theme/globals/react-flow.ts'

const themeHiveMQ = extendTheme({
  fonts: {
    heading: `'Roboto', sans-serif`,
    body: `'Roboto', sans-serif`,
  },
  styles: {
    global: {
      ...treeView,
      ...reactFlow,
    },
  },

  colors: colors,
  semanticTokens: semanticTokens,
  components,
})

export default themeHiveMQ
