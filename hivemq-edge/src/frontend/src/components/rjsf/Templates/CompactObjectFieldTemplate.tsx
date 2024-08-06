import { FC } from 'react'
import { ObjectFieldTemplateProps, RJSFSchema } from '@rjsf/utils'
import { Td } from '@chakra-ui/react'

import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

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
