import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import PageContainer from '@/components/PageContainer.tsx'

const EdgeFlowPage: FC = () => {
  const { t } = useTranslation()

  return (
    <PageContainer title={t('welcome.title') as string} subtitle={t('welcome.description') as string}></PageContainer>
  )
}

export default EdgeFlowPage
