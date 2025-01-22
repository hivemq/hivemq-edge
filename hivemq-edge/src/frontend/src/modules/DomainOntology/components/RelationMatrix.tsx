import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import ChordChart from '@/modules/DomainOntology/components/charts/ChordChart.tsx'
import { useGetChordMatrixData } from '@/modules/DomainOntology/hooks/useGetChordMatrixData.ts'

const RelationMatrix: FC = () => {
  const { t } = useTranslation()
  const { matrixData, isError } = useGetChordMatrixData()

  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper data-testid="edge-panel-relation-matrix">
      <ChordChart matrix={matrixData.matrix} keys={matrixData.keys}></ChordChart>
    </ChartWrapper>
  )
}

export default RelationMatrix
