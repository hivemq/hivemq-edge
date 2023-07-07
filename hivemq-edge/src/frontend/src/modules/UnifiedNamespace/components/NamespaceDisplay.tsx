import { FC } from 'react'
import { Breadcrumb, BreadcrumbItem, type BreadcrumbProps, Text } from '@chakra-ui/react'
import { ISA95ApiBean } from '@/api/__generated__'

interface NamespaceDisplayProps extends Pick<BreadcrumbProps, 'fontSize'> {
  namespace: ISA95Namespace
}

type ISA95Namespace = Pick<ISA95ApiBean, 'enterprise' | 'site' | 'area' | 'productionLine' | 'workCell'>

// eslint-disable-next-line react-refresh/only-export-components
export const toArray = (namespace: ISA95Namespace): string[] => {
  const { enterprise, site, area, workCell, productionLine } = namespace
  const breadcrumb = [enterprise, site, area, productionLine, workCell]
  return breadcrumb.filter((element): element is string => !!element)
}

const NamespaceDisplay: FC<NamespaceDisplayProps> = ({ namespace, fontSize = '2xl' }) => {
  const color = fontSize === '2xl' ? 'gray.500' : 'black'

  // TODO[NVL] Maybe not a good idea to use breadcrumb as it adds role=nav
  return (
    <Breadcrumb
      sx={{
        ol: { flexWrap: 'wrap' },
      }}
    >
      {toArray(namespace).map((e, i) => (
        <BreadcrumbItem color={color} fontWeight={600} fontSize={fontSize}>
          <Text key={`element-${i}`}>{e}</Text>
        </BreadcrumbItem>
      ))}
    </Breadcrumb>
  )
}

export default NamespaceDisplay
