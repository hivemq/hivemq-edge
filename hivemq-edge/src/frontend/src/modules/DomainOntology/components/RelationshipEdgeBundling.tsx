import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import { useGetTreeData } from '@/modules/DomainOntology/hooks/useGetTreeData.ts'
import { HierarchicalEdgeBundling } from '@/modules/DomainOntology/components/charts/HierarchicalEdgeBundling.tsx'

const RelationshipEdgeBundling: FC = () => {
  const { t } = useTranslation()
  const { treeData, isError } = useGetTreeData()

  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper>
      <HierarchicalEdgeBundling data={treeData} width={600} height={600} />
    </ChartWrapper>
  )
}

export default RelationshipEdgeBundling
