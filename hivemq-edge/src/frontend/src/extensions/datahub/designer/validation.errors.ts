import type { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import type { Node } from '@xyflow/react'
import type { DataHubNodeType } from '@datahub/types.ts'
import i18n from '@/config/i18n.config.ts'

const commonProperties: Pick<ProblemDetailsExtended, 'status'> = { status: 404 }

export const PolicyCheckErrors = {
  notConnected: (source: DataHubNodeType, target: Node, handle?: string) => ({
    title: target.type as string,
    detail: handle
      ? i18n.t('datahub:error.dryRun.noHandleConnected', { source, target: target.type, handle })
      : i18n.t('datahub:error.dryRun.notConnected', { source, target: target.type }),
    type: 'datahub.notConnected',
    ...commonProperties,
    id: target.id,
  }),
  cardinality: (source: DataHubNodeType, target: Node) => ({
    title: target.type as string,
    detail: i18n.t('datahub:error.dryRun.cardinality', { source, target: target.type }),
    type: 'datahub.cardinality',
    ...commonProperties,
    id: target.id,
  }),
  // TODO[NVL] For properties, can we use Pick<T> and link to the exact widget in the form?
  notConfigured: (source: Node, properties: string) => ({
    title: source.type as string,
    detail: i18n.t('datahub:error.dryRun.notConfigured', { source: source.type, properties: properties }),
    type: 'datahub.notConfigured',
    ...commonProperties,
    id: source.id,
  }),
  notValidated: (source: Node, error: string) => {
    return {
      title: source.type as string,
      detail: i18n.t('datahub:error.dryRun.notValidated', { source: source.type, error }),
      type: 'datahub.notConfigured',
      ...commonProperties,
      id: source.id,
    }
  },
  internal: (source: Node, error: unknown) => {
    let message: string
    if (error instanceof Error) message = error.message
    else message = String(error)

    return {
      title: source.type as string,
      detail: i18n.t('datahub:error.dryRun.internal', { source: source.type, error: message }),
      type: 'datahub.notConfigured',
      ...commonProperties,
      id: source.id,
    }
  },
}
