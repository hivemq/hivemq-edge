import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import ChordChart from '@/modules/DomainOntology/components/charts/ChordChart.tsx'
import { useGetChordMatrixData } from '@/modules/DomainOntology/hooks/useGetChordMatrixData.ts'

const RelationMatrix: FC = () => {
  const { t } = useTranslation()
  const { matrixData, isError, isLoading } = useGetChordMatrixData()

  if (isLoading) return <LoaderSpinner />
  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper data-testid="edge-panel-relation-matrix">
      <ChordChart matrix={matrixData.matrix} keys={matrixData.keys} />
    </ChartWrapper>
  )
}

export default RelationMatrix
