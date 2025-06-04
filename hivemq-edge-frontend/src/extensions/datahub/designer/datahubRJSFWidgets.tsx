import type { RegistryWidgetsType } from '@rjsf/utils'

import { AdapterSelect, JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from '@datahub/components/forms'
import FunctionCreatableSelect from '@datahub/components/forms/FunctionCreatableSelect.tsx'
import { MetricCounterInput } from '@datahub/components/forms/MetricCounterInput.tsx'
import { VersionManagerSelect } from '@datahub/components/forms/VersionManagerSelect.tsx'
import { MessageInterpolationTextArea } from '@datahub/components/forms/MessageInterpolationTextArea.tsx'
import { TransitionSelect } from '@datahub/components/forms/TransitionSelect.tsx'
import {
  SchemaNameCreatableSelect,
  ScriptNameCreatableSelect,
} from '@datahub/components/forms/ResourceNameCreatableSelect.tsx'

export const datahubRJSFWidgets: RegistryWidgetsType = {
  'application/schema+json': JSONSchemaEditor,
  'text/javascript': JavascriptEditor,
  'application/octet-stream': ProtoSchemaEditor,
  'datahub:function-selector': FunctionCreatableSelect,
  'datahub:transition-selector': TransitionSelect,
  'datahub:metric-counter': MetricCounterInput,
  'datahub:function-name': ScriptNameCreatableSelect,
  'datahub:schema-name': SchemaNameCreatableSelect,
  'datahub:version': VersionManagerSelect,
  'datahub:message-interpolation': MessageInterpolationTextArea,
  'edge:adapter-selector': AdapterSelect,
}
