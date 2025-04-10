import type { FC } from 'react'
import type { FieldProps } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils'

import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { CompactArrayFieldTemplate } from '@/components/rjsf/Templates/CompactArrayFieldTemplate.tsx'
import { CompactFieldTemplate } from '@/components/rjsf/Templates/CompactFieldTemplate.tsx'
import { CompactBaseInputTemplate } from '@/components/rjsf/Templates/CompactBaseInputTemplate.tsx'
import { CompactObjectFieldTemplate } from '@/components/rjsf/Templates/CompactObjectFieldTemplate.tsx'
import { CompactArrayFieldItemTemplate } from '@/components/rjsf/Templates/CompactArrayFieldItemTemplate.tsx'

const CompactArrayField: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { registry } = props

  registry.templates = {
    ...registry.templates,
    ArrayFieldTemplate: CompactArrayFieldTemplate,
    ArrayFieldItemTemplate: CompactArrayFieldItemTemplate,
    FieldTemplate: CompactFieldTemplate,
    BaseInputTemplate: CompactBaseInputTemplate,
    ObjectFieldTemplate: CompactObjectFieldTemplate,
  }

  return <props.registry.fields.ArrayField {...props} />
}

export default CompactArrayField
