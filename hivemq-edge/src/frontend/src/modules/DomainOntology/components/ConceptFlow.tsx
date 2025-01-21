import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import SankeyChart from '@/modules/DomainOntology/components/charts/SankeyChart.tsx'
import { useGetSankeyData } from '@/modules/DomainOntology/hooks/useGetSankeyData.ts'

const ConceptFlow: FC = () => {
  const { t } = useTranslation()
  const { sankeyData, isError } = useGetSankeyData()

  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper>
      <SankeyChart data={sankeyData}></SankeyChart>
    </ChartWrapper>
  )
}

export default ConceptFlow
