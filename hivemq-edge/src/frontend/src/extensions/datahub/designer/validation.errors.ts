import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import { Node } from 'reactflow'
import { DataHubNodeType } from '@datahub/types.ts'
import i18n from '@/config/i18n.config.ts'

const commonProperties: Pick<ProblemDetailsExtended, 'status'> = { status: 404 }

export const PolicyCheckErrors = {
  notConnected: <T>(from: DataHubNodeType, to: Node<T>) => ({
    title: to.type as string,
    detail: i18n.t('datahub:error.dryRun.notConnected', { source: from, target: to.type }),
    type: 'datahub.notConnected',
    ...commonProperties,
    id: to.id,
  }),
  cardinality: <T>(from: DataHubNodeType, to: Node<T>) => ({
    title: to.type as string,
    detail: i18n.t('datahub:error.dryRun.cardinality', { source: from, target: to.type }),
    type: 'datahub.cardinality',
    ...commonProperties,
    id: to.id,
  }),
}
