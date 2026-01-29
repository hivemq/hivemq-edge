import type { RegistryWidgetsType } from '@rjsf/utils'

import { AdapterSelect, JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from '@datahub/components/forms'
import FunctionCreatableSelect from '@datahub/components/forms/FunctionCreatableSelect.tsx'
import { MetricCounterInput } from '@datahub/components/forms/MetricCounterInput.tsx'
import { VersionManagerSelect } from '@datahub/components/forms/VersionManagerSelect.tsx'
import { MessageInterpolationTextArea } from '@datahub/components/forms/MessageInterpolationTextArea.tsx'
import { MessageTypeSelect } from '@datahub/components/forms/MessageTypeSelect.tsx'
import { TransitionSelect } from '@datahub/components/forms/TransitionSelect.tsx'
import { BehaviorModelSelect } from '@datahub/components/forms/BehaviorModelSelect.tsx'
import { BehaviorModelSelectDropdown } from '@datahub/components/forms/BehaviorModelSelectDropdown.tsx'
import { BehaviorModelReadOnlyDisplay } from '@datahub/components/forms/BehaviorModelReadOnlyDisplay.tsx'
import {
  SchemaNameCreatableSelect,
  ScriptNameCreatableSelect,
  SchemaNameSelect,
  ScriptNameSelect,
} from '@datahub/components/forms/ResourceNameCreatableSelect.tsx'

export const datahubRJSFWidgets: RegistryWidgetsType = {
  'application/schema+json': JSONSchemaEditor,
  'text/javascript': JavascriptEditor,
  'application/octet-stream': ProtoSchemaEditor,
  'datahub:function-selector': FunctionCreatableSelect,
  'datahub:transition-selector': TransitionSelect,
  'datahub:behavior-model-selector': BehaviorModelSelectDropdown, // Dropdown version (default)
  'datahub:behavior-model-selector-radio': BehaviorModelSelect, // Radio card version (alternative)
  'datahub:behavior-model-selector-dropdown': BehaviorModelSelectDropdown, // Explicit dropdown version
  'datahub:behavior-model-readonly': BehaviorModelReadOnlyDisplay, // Read-only display for model
  'datahub:metric-counter': MetricCounterInput,
  'datahub:function-name': ScriptNameCreatableSelect,
  'datahub:schema-name': SchemaNameCreatableSelect,
  'datahub:function-name-select': ScriptNameSelect, // Select-only version (no create)
  'datahub:schema-name-select': SchemaNameSelect, // Select-only version (no create)
  'datahub:version': VersionManagerSelect,
  'datahub:message-interpolation': MessageInterpolationTextArea,
  'datahub:message-type': MessageTypeSelect, // Protobuf message type selector
  'edge:adapter-selector': AdapterSelect,
}
