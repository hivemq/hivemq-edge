import { ISA95Namespace } from '@/modules/UnifiedNamespace/types.ts'

export const namespaceToStrings = (namespace: ISA95Namespace): string[] => {
  const { enterprise, site, area, workCell, productionLine } = namespace
  const breadcrumb = [enterprise, site, area, productionLine, workCell]
  return breadcrumb.filter((element): element is string => !!element)
}

export const NAMESPACE_SEPARATOR = '/'
