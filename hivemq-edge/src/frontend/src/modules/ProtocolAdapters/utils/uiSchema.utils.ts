import { UiSchema } from '@rjsf/utils'

/* istanbul ignore next -- @preserve */
export const defaultUiSchema: UiSchema = {
  // required to relocate the submit button outside the form
  'ui:submitButtonOptions': {
    norender: false,
  },
}
