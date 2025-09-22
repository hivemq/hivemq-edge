import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useGetBridge } from '@/api/hooks/useGetBridges/useGetBridge'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import ErrorMessage from '@/components/ErrorMessage'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo'
import { NodeTypes } from '@/modules/Workspace/types'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard'

interface EntityRendererProps {
  reference: EntityReference
}

const AdapterEntityRenderer: FC<EntityRendererProps> = ({ reference }) => {
  const { t } = useTranslation()
  const { isLoading, adapter, protocol } = useGetAdapterInfo(reference.id)

  if (isLoading) return <LoaderSpinner />
  if (!adapter) return <ErrorMessage message={t('combiner.error.noValidReference')} />
  return (
    <NodeNameCard
      type={NodeTypes.ADAPTER_NODE}
      name={adapter.id}
      icon={protocol?.logoUrl}
      description={protocol?.name}
    />
  )
}

const BridgeEntityRenderer: FC<EntityRendererProps> = ({ reference }) => {
  const { t } = useTranslation()
  const { data, isLoading } = useGetBridge(reference.id)

  if (isLoading) return <LoaderSpinner />
  if (!data) return <ErrorMessage message={t('combiner.error.noValidReference')} />
  return <NodeNameCard type={NodeTypes.BRIDGE_NODE} name={data?.id} />
}

const BrokerEntityRenderer: FC<EntityRendererProps> = () => {
  const { t } = useTranslation()
  return (
    <NodeNameCard
      type={NodeTypes.EDGE_NODE}
      name={t('branding.appName')}
      description={t('combiner.schema.sources.edge.description')}
    />
  )
}

const PulseAgentEntityRenderer: FC<EntityRendererProps> = () => {
  const { t } = useTranslation()
  return (
    <NodeNameCard
      type={NodeTypes.PULSE_NODE}
      name={t('combiner.schema.sources.pulse.name')}
      description={t('combiner.schema.sources.pulse.description')}
    />
  )
}

export const EntityRenderer: FC<EntityRendererProps> = ({ reference }) => {
  const { t } = useTranslation()
  if (reference.type === EntityType.BRIDGE) return <BridgeEntityRenderer reference={reference} />
  if (reference.type === EntityType.ADAPTER) return <AdapterEntityRenderer reference={reference} />
  if (reference.type === EntityType.EDGE_BROKER) return <BrokerEntityRenderer reference={reference} />
  if (reference.type === EntityType.PULSE_AGENT) return <PulseAgentEntityRenderer reference={reference} />
  return <ErrorMessage message={t('combiner.error.noValidReference')} />
}
