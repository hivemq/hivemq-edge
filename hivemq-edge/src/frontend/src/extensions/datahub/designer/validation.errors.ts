import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import { Node } from 'reactflow'
import { DataHubNodeType } from '@datahub/types.ts'
import i18n from '@/config/i18n.config.ts'

const commonProperties: Pick<ProblemDetailsExtended, 'status'> = { status: 404 }

export const PolicyCheckErrors = {
  notConnected: <T>(source: DataHubNodeType, target: Node<T>, handle?: string) => ({
    title: target.type as string,
    detail: handle
      ? i18n.t('datahub:error.dryRun.noHandleConnected', { source, target: target.type, handle })
      : i18n.t('datahub:error.dryRun.notConnected', { source, target: target.type }),
    type: 'datahub.notConnected',
    ...commonProperties,
    id: target.id,
  }),
  cardinality: <T>(source: DataHubNodeType, target: Node<T>) => ({
    title: target.type as string,
    detail: i18n.t('datahub:error.dryRun.cardinality', { source, target: target.type }),
    type: 'datahub.cardinality',
    ...commonProperties,
    id: target.id,
  }),
  // TODO[NVL] For properties, can we use Pick<T> and link to the exact widget in the form?
  notConfigured: <T>(source: Node<T>, properties: string) => ({
    title: source.type as string,
    detail: i18n.t('datahub:error.dryRun.notConfigured', { source: source.type, properties: properties }),
    type: 'datahub.notConfigured',
    ...commonProperties,
    id: source.id,
  }),
}
