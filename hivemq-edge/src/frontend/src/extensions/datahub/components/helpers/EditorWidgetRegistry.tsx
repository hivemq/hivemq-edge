import { RegistryWidgetsType } from '@rjsf/utils'

import { VersionManagerSelect } from './VersionManagerSelect.tsx'
import FunctionCreatableSelect from './FunctionCreatableSelect.tsx'
import { JsFunctionInput, MetricCounterInput } from './MetricCounterInput.tsx'
import { JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from './CodeEditor.tsx'
import { MessageInterpolationTextArea } from './MessageInterpolationTextArea.tsx'
import { AdapterSelect } from './AdapterSelect.tsx'

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
