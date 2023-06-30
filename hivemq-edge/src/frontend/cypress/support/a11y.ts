import { RuleObject } from 'axe-core'

export const ignoreGlobalRules: { rules: RuleObject } = {
  rules: {
    // problem with lib, see https://github.com/chakra-ui/chakra-ui/pull/6845
    'landmark-unique': { enabled: false },
  },
}
