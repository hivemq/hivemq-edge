import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import PageContainer from '@/components/PageContainer.tsx'

import { EdgeFlowProvider } from './hooks/FlowContext.tsx'
import ReactFlowWrapper from './components/ReactFlowWrapper.tsx'
import WorkspaceOptionsDrawer from './components/drawers/WorkspaceOptionsDrawer.tsx'

const EdgeFlowPage: FC = () => {
  const { t } = useTranslation()

  return (
    <PageContainer title={t('welcome.title')} subtitle={t('welcome.description')}>
      <EdgeFlowProvider>
        <ReactFlowWrapper />
        <WorkspaceOptionsDrawer />
      </EdgeFlowProvider>
    </PageContainer>
  )
}

export default EdgeFlowPage
