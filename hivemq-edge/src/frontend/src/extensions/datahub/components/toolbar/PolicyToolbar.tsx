import { FC } from 'react'

import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'

import { ToolbarDryRun } from '@datahub/components/toolbar/ToolbarDryRun.tsx'
import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish.tsx'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { ToolbarClear } from '@datahub/components/toolbar/ToolbarClear.tsx'
import { ToolbarShowReport } from '@datahub/components/toolbar/ToolbarShowReport.tsx'

const PolicyToolbar: FC = () => {
  const { report } = usePolicyChecksStore()

  return (
    <ToolbarButtonGroup orientation="horizontal" variant="outline" gap="0.5em">
      <ToolbarDryRun />
      <ToolbarButtonGroup isAttached variant="outline">
        <ToolbarShowReport />
        {report && <ToolbarClear />}
      </ToolbarButtonGroup>

      <ToolbarPublish />
    </ToolbarButtonGroup>
  )
}

export default PolicyToolbar
