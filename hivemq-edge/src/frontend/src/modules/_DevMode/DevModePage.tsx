import { FC } from 'react'
import { Outlet } from 'react-router-dom'

import PageContainer from '@/components/PageContainer.tsx'

const DevModePage: FC = () => {
  return (
    <PageContainer title="Sandbox">
      <Outlet />
    </PageContainer>
  )
}

export default DevModePage
