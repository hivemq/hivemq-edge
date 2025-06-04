import { alertAnatomy } from '@chakra-ui/anatomy'
import { createMultiStyleConfigHelpers, theme as baseTheme, cssVar } from '@chakra-ui/react'

const { definePartsStyle, defineMultiStyleConfig } = createMultiStyleConfigHelpers(alertAnatomy.keys)

const $fg = cssVar('alert-fg')
const $bg = cssVar('alert-bg')

const lightVariant = definePartsStyle((props) => {
  const { colorScheme: c } = props
  return {
    container: {
      [$fg.variable]: `colors.${c}.600`,
      [$bg.variable]: 'unset',
      _dark: {
        [$fg.variable]: `colors.${c}.200`,
        [$bg.variable]: 'unset',
      },
    },
  }
})

export const alertTheme = defineMultiStyleConfig({
  variants: { ...baseTheme.components.Alert.variants, light: lightVariant },
})
