import { FC, useMemo } from 'react'
import { ColumnDef } from '@tanstack/react-table'
import { DateTime } from 'luxon'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import { Button, Skeleton, Text } from '@chakra-ui/react'
import { FaEdit } from 'react-icons/fa'
import { BiAddToQueue } from 'react-icons/bi'

import DateTimeRenderer from '@/components/DateTime/DateTimeRenderer.tsx'
import PaginatedTable from '@/components/PaginatedTable/PaginatedTable.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

import { BehaviorPolicy, BehaviorPolicyMatching, DataPolicy, DataPolicyMatching } from '@/api/__generated__'
import { mockDataPolicy } from '../../api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '../../api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { useGetAllDataPolicies } from '../../api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.tsx'
import { useGetAllBehaviorPolicies } from '../../api/hooks/DataHubBehaviorPoliciesService/useGetAllBehaviorPolicies.tsx'
import { PolicyType } from '../../types.ts'

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
        header: t('policyTable.header.type') as string,
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
        header: t('policyTable.header.matching') as string,
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
        header: t('policyTable.header.created') as string,
      },
      {
        id: 'actions',
        header: t('policyTable.header.actions') as string,
        sortingFn: undefined,
        cell: (info) => {
          return (
            <Skeleton isLoaded={!isLoading}>
              <IconButton
                size="sm"
                onClick={() => navigate(`/datahub/${info.row.original.type}/id`)}
                aria-label={t('policyTable.action.edit')}
                icon={<FaEdit />}
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
            {t('policyTable.action.create')}
          </Button>
        ),
      },
    ]
  }, [isLoading, navigate, t])

  return (
    <PaginatedTable<CombinedPolicy>
      aria-label={t('policyTable.title')}
      data={safeData}
      columns={columns}
      isError={isError}
      // enablePagination={variant === 'full'}
      // enableColumnFilters={variant === 'full'}
      // getRowStyles={(row) => {
      //   return { backgroundColor: theme.colors.blue[50] }
      // }}
    />
  )
}

export default PolicyTable
