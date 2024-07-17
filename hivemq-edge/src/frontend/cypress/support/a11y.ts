import { RuleObject } from 'axe-core'

export const ignoreGlobalRules: { rules: RuleObject } = {
  rules: {
    // problem with Chakra UI, see https://github.com/chakra-ui/chakra-ui/pull/6845
    'landmark-unique': { enabled: false },
    // False positives. Check with the config
    'landmark-one-main': { enabled: false },
    'page-has-heading-one': { enabled: false },
  },
}
