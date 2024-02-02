import { RegistryWidgetsType } from '@rjsf/utils'

import { AdapterSelect, JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from '@datahub/components/forms'
import FunctionCreatableSelect from '@datahub/components/forms/FunctionCreatableSelect.tsx'
import { JsFunctionInput, MetricCounterInput } from '@datahub/components/forms/MetricCounterInput.tsx'
import { VersionManagerSelect } from '@datahub/components/forms/VersionManagerSelect.tsx'
import { MessageInterpolationTextArea } from '@datahub/components/forms/MessageInterpolationTextArea.tsx'

export const datahubRJSFWidgets: RegistryWidgetsType = {
  'application/schema+json': JSONSchemaEditor,
  'text/javascript': JavascriptEditor,
  'application/octet-stream': ProtoSchemaEditor,
  'datahub:function-selector': FunctionCreatableSelect,
  'datahub:metric-counter': MetricCounterInput,
  'datahub:function-name': JsFunctionInput,
  'datahub:version': VersionManagerSelect,
  'datahub:message-interpolation': MessageInterpolationTextArea,
  'edge:adapter-selector': AdapterSelect,
}
