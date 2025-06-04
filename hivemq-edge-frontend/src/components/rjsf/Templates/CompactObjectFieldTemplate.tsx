import type { FC } from 'react'
import type { ObjectFieldTemplateProps, RJSFSchema } from '@rjsf/utils'
import { Td } from '@chakra-ui/react'

import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

export const CompactObjectFieldTemplate: FC<ObjectFieldTemplateProps<unknown, RJSFSchema, AdapterContext>> = (
  props
) => {
  const { idSchema, properties } = props

  return (
    <>
      {properties.map((element, index) => (
        <Td key={`${idSchema.$id}-${element.name}-${index}`}>{element.content}</Td>
      ))}
    </>
  )
}
