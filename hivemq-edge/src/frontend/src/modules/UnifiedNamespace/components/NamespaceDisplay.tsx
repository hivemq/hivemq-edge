import { FC } from 'react'
import { Breadcrumb, BreadcrumbItem, type BreadcrumbProps, Text } from '@chakra-ui/react'
import { ISA95Namespace } from '@/modules/UnifiedNamespace/types.ts'

import { NAMESPACE_SEPARATOR, namespaceToStrings } from '../namespace-utils.ts'

interface NamespaceDisplayProps extends Pick<BreadcrumbProps, 'fontSize'> {
  namespace: ISA95Namespace
}

const NamespaceDisplay: FC<NamespaceDisplayProps> = ({ namespace, fontSize = '2xl' }) => {
  const color = fontSize === '2xl' ? 'gray.500' : 'black'

  // TODO[NVL] Maybe not a good idea to use breadcrumb as it adds role=nav
  return (
    <Breadcrumb
      separator={NAMESPACE_SEPARATOR}
      sx={{
        ol: { flexWrap: 'wrap' },
      }}
    >
      {namespaceToStrings(namespace).map((e, i) => (
        <BreadcrumbItem key={`element-${i}`} color={color} fontWeight={600} fontSize={fontSize}>
          <Text>{e}</Text>
        </BreadcrumbItem>
      ))}
    </Breadcrumb>
  )
}

export default NamespaceDisplay
