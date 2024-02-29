import { FC, useMemo } from 'react'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import { Button, Skeleton, Text } from '@chakra-ui/react'
import { BiAddToQueue } from 'react-icons/bi'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'

import { BehaviorPolicy, BehaviorPolicyMatching, DataPolicy, DataPolicyMatching } from '@/api/__generated__'
import { PolicyType } from '@datahub/types.ts'
import { useGetAllBehaviorPolicies } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useGetAllBehaviorPolicies.tsx'
import { useDeleteDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useDeleteDataPolicy.tsx'
import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'
import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'

type CombinedPolicy = (DataPolicy & { type: PolicyType }) | (BehaviorPolicy & { type: PolicyType })

const PolicyTable: FC = () => {
  const { t } = useTranslation('datahub')
  const { isLoading: isDataLoading, data: dataPolicies, isError: isDataError } = useGetAllDataPolicies()
  const {
    isLoading: isBehaviorLoading,
    data: behaviorPolicies,
    isError: isBehaviorError,
  } = useGetAllBehaviorPolicies({})
  const navigate = useNavigate()
  const deletePolicy = useDeleteDataPolicy()

  const isError = useMemo(() => {
    return isDataError || isBehaviorError
  }, [isDataError, isBehaviorError])

  const isLoading = useMemo(() => {
    return isDataLoading || isBehaviorLoading
  }, [isDataLoading, isBehaviorLoading])

  const safeData = useMemo<CombinedPolicy[]>(() => {
    if (isLoading)
      return [
        { ...mockDataPolicy, type: PolicyType.DATA },
        { ...mockBehaviorPolicy, type: PolicyType.BEHAVIOR },
      ]

    return [
      ...(dataPolicies?.items
        ? dataPolicies.items.map((e) => ({ ...e, type: PolicyType.DATA } as CombinedPolicy))
        : []),
      ...(behaviorPolicies?.items
        ? behaviorPolicies.items.map((e) => ({ ...e, type: PolicyType.BEHAVIOR } as CombinedPolicy))
        : []),
    ]
  }, [isLoading, dataPolicies, behaviorPolicies])

  const columns = useMemo<ColumnDef<CombinedPolicy>[]>(() => {
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
        header: t('Listings.policy.header.type') as string,
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
        header: t('Listings.policy.header.matching') as string,
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
        header: t('Listings.policy.header.created') as string,
      },
      {
        id: 'actions',
        header: t('Listings.policy.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <DataHubListAction
                onDelete={() =>
                  deletePolicy
                    .mutateAsync(info.row.original.id)
                    .then((e) => console.log('XXXXXX', e))
                    .catch((e) => console.log('XXXXX', e.toString(), info))
                }
                onEdit={() => navigate(`/datahub/${info.row.original.type}/id`)}
              />
            </Skeleton>
          )
        },
        footer: () => (
          <Button
            leftIcon={<BiAddToQueue />}
            onClick={() => navigate(`/datahub/${PolicyType.CREATE_POLICY}`)}
            variant="primary"
          >
            {t('Listings.policy.action.create')}
          </Button>
        ),
      },
    ]
  }, [deletePolicy, isLoading, navigate, t])

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
