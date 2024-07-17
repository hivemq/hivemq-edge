import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import { FC } from 'react'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

type FormDataValue = string
type FormDataItem = Record<string, FormDataValue>
type FormData = Record<number, FormDataItem>

const CompactArrayField: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { schema, formData } = props
  console.log('XXXXXX', formData as FormData)

  // TODO Check for other conditions on the Schema and UISchema
  if (schema.type !== 'array') return <props.registry.fields.ArrayField {...props} />

  return <></>
}

export default CompactArrayField
