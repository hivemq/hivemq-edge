import { RegistryWidgetsType, UiSchema } from '@rjsf/utils'

export const getRequiredUiSchema = (uiSchema: UiSchema | undefined, isNewAdapter: boolean): UiSchema => {
  const { ['ui:submitButtonOptions']: submitButtonOptions, id, ...rest } = uiSchema || {}
  return {
    'ui:submitButtonOptions': {
      // required to relocate the submit button outside the form
      ...submitButtonOptions,
      norender: true,
    },
    id: {
      // required to prevent custom validation when editing an existing adapter
      ...id,
      'ui:disabled': !isNewAdapter,
    },
    ...rest,
  }
}

export const adapterJSFWidgets: RegistryWidgetsType = {
  // @ts-ignore [24369] Turn discovery browser off (and replace by regular text input)
  'discovery:tagBrowser': 'text',
}
