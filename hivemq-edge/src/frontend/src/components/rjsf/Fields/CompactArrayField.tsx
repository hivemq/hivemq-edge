import { FC } from 'react'
import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { CompactArrayFieldTemplate } from '@/components/rjsf/Templates/CompactArrayFieldTemplate.tsx'
import { CompactFieldTemplate } from '@/components/rjsf/Templates/CompactFieldTemplate.tsx'
import { CompactBaseInputTemplate } from '@/components/rjsf/Templates/CompactBaseInputTemplate.tsx'
import { CompactObjectFieldTemplate } from '@/components/rjsf/Templates/CompactObjectFieldTemplate.tsx'
import { CompactArrayFieldItemTemplate } from '@/components/rjsf/Templates/CompactArrayFieldItemTemplate.tsx'

const CompactArrayField: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { registry } = props

  const template = {
    ...registry.templates,
    ArrayFieldTemplate: CompactArrayFieldTemplate,
    ArrayFieldItemTemplate: CompactArrayFieldItemTemplate,
    FieldTemplate: CompactFieldTemplate,
    BaseInputTemplate: CompactBaseInputTemplate,
    ObjectFieldTemplate: CompactObjectFieldTemplate,
  }
  const compactRegistry = { ...registry, template: template }

  return <props.registry.fields.ArrayField {...props} registry={compactRegistry} />
}

export default CompactArrayField
