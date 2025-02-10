import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import PageContainer from '@/components/PageContainer.tsx'

import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider.tsx'
import ReactFlowWrapper from '@/modules/Workspace/components/ReactFlowWrapper.tsx'
import WorkspaceOptionsDrawer from '@/modules/Workspace/components/drawers/WorkspaceOptionsDrawer.tsx'

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
