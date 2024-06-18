import { RegistryWidgetsType, UiSchema } from '@rjsf/utils'
import AdapterTagSelect from '@/components/rjsf/Widgets/AdapterTagSelect.tsx'

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
  'discovery:tagBrowser': AdapterTagSelect,
}
