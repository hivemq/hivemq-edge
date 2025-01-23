import type { FC } from 'react'
import { useMemo } from 'react'
import type { CellContext, ColumnDef } from '@tanstack/react-table'
import type { UseMutationResult } from '@tanstack/react-query'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Skeleton, Text } from '@chakra-ui/react'

import type { BehaviorPolicy, BehaviorPolicyMatching, DataPolicy, DataPolicyMatching } from '@/api/__generated__'
import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'

import type { CombinedPolicy } from '@datahub/types.ts'
import { PolicyType } from '@datahub/types.ts'
import { useGetAllBehaviorPolicies } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useGetAllBehaviorPolicies.tsx'
import { useDeleteDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useDeleteDataPolicy.tsx'
import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'
import { useDeleteBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useDeleteBehaviorPolicy.tsx'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'
import type { DataHubTableProps } from '@datahub/components/pages/DataHubListings.tsx'
import { downloadJSON } from '@datahub/utils/download.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const PolicyTable: FC<DataHubTableProps> = ({ onDeleteItem }) => {
  const { t } = useTranslation('datahub')
  const { isLoading: isDataLoading, data: dataPolicies, isError: isDataError } = useGetAllDataPolicies()
  const {
    isLoading: isBehaviorLoading,
    data: behaviorPolicies,
    isError: isBehaviorError,
  } = useGetAllBehaviorPolicies({})
  const navigate = useNavigate()
  const deleteDataPolicy = useDeleteDataPolicy()
  const deleteBehaviourPolicy = useDeleteBehaviorPolicy()
  const { isDirty, reset } = useDataHubDraftStore()

  const isError = useMemo(() => {
    return isDataError || isBehaviorError
  }, [isDataError, isBehaviorError])

  const isLoading = useMemo(() => {
    return isDataLoading || isBehaviorLoading
  }, [isDataLoading, isBehaviorLoading])

  const isDraftDirty = isDirty()

  const safeData = useMemo<CombinedPolicy[]>(() => {
    if (isLoading)
      return [
        { ...mockDataPolicy, type: PolicyType.DATA_POLICY },
        { ...mockBehaviorPolicy, type: PolicyType.BEHAVIOR_POLICY },
      ]

    const generateDraftPolicyItem = (): CombinedPolicy => ({
      type: PolicyType.CREATE_POLICY,
      id: '',
      createdAt: DateTime.now().toISO() as string,
      matching: { topicFilter: '' },
    })

    function insertItems<Item extends DataPolicy | BehaviorPolicy>(
      items: Item[] | undefined,
      type: PolicyType
    ): CombinedPolicy[] {
      if (!items) return []
      return items.map((e) => ({ ...e, type } as CombinedPolicy))
    }

    const ss = [
      ...insertItems<DataPolicy>(dataPolicies?.items, PolicyType.DATA_POLICY),
      ...insertItems<BehaviorPolicy>(behaviorPolicies?.items, PolicyType.BEHAVIOR_POLICY),
    ]
    if (isDraftDirty) ss.unshift(generateDraftPolicyItem())

    return ss
  }, [isLoading, isDraftDirty, dataPolicies?.items, behaviorPolicies?.items])

  const columns = useMemo<ColumnDef<CombinedPolicy>[]>(() => {
    const onHandleDelete = (info: CellContext<CombinedPolicy, unknown>) => {
      if (info.row.original.type === PolicyType.CREATE_POLICY) {
        reset()
        return
      }

      const deleteMutation: UseMutationResult<void, unknown, string> =
        info.row.original.type === PolicyType.DATA_POLICY ? deleteDataPolicy : deleteBehaviourPolicy

      onDeleteItem?.(deleteMutation.mutateAsync, info.row.original.type, info.row.original.id)
    }

    const onHandleEdit = (info: CellContext<CombinedPolicy, unknown>) => () => {
      navigate(`/datahub/${info.row.original.type}/${info.row.original.id}`)
    }

    const onHandleDownload = (info: CellContext<CombinedPolicy, unknown>) => () => {
      const { type, ...policy } = info.row.original
      downloadJSON<DataPolicy | BehaviorPolicy>(info.row.original.id, policy)
    }

    return [
      {
        accessorKey: 'id',
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <Text>{info.getValue<string>()}</Text>
          </Skeleton>
        ),
      },
      {
        accessorKey: 'type',
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>{t('policy.type', { context: info.getValue<string>() })}</Text>
            </Skeleton>
          )
        },
        header: t('Listings.policy.header.type'),
      },
      {
        accessorKey: 'matching',
        cell: (info) => {
          const matching = info.getValue<DataPolicyMatching | BehaviorPolicyMatching>()
          return (
            <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
              <Text>
                {(matching as DataPolicyMatching).topicFilter || (matching as BehaviorPolicyMatching).clientIdRegex}
              </Text>
            </Skeleton>
          )
        },
        header: t('Listings.policy.header.matching'),
      },
      {
        accessorKey: 'createdAt',
        sortType: 'datetime',
        accessorFn: (row) => (row.createdAt ? DateTime.fromISO(row.createdAt).toMillis() : undefined),
        cell: (info) => (
          <Skeleton isLoaded={!isLoading} whiteSpace="nowrap">
            <DateTimeRenderer date={DateTime.fromMillis(info.getValue() as number)} isApprox />
          </Skeleton>
        ),
        header: t('Listings.policy.header.created'),
      },
      {
        id: 'actions',
        header: t('Listings.policy.header.actions'),
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                policy={info.row.original}
                onDelete={() => onHandleDelete(info)}
                onEdit={onHandleEdit(info)}
                onDownload={onHandleDownload(info)}
              />
            </Skeleton>
          )
        },
      },
    ]
  }, [deleteBehaviourPolicy, deleteDataPolicy, isLoading, navigate, onDeleteItem, reset, t])

  return (
    <PaginatedTable<CombinedPolicy>
      aria-label={t('Listings.policy.label')}
      data={safeData}
      columns={columns}
      isError={isError}
    />
  )
}

export default PolicyTable
