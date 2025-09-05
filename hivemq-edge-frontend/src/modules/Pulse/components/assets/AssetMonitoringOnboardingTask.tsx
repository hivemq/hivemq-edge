import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Link as RouterLink } from 'react-router-dom'
import { Badge, Button, List, ListItem, Text } from '@chakra-ui/react'
import { CheckIcon } from '@chakra-ui/icons'

import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface AssetMonitoringOnboardingTaskContent {
  [key: string]: (count: number) => { name: string; color?: string; searchParams?: string }
}

const AssetMonitoringOnboardingTask: FC = () => {
  const { t } = useTranslation()
  const { data: assets, isLoading: isAssetLoading, error } = useListManagedAssets()

  // TODO[NVL] Colors need to be sync with other Pulse/asset components and the theme
  const categories: AssetMonitoringOnboardingTaskContent = {
    unmapped: (count: number) => ({
      name: t('pulse.onboarding.monitoring.unmapped', { count: count }),
      color: 'status.disconnected',
      searchParams: `status=${AssetMapping.status.UNMAPPED}`,
    }),
    remapping: (count: number) => ({
      name: t('pulse.onboarding.monitoring.remapping', { count: count }),
      color: 'status.error',
      searchParams: `status=${AssetMapping.status.REQUIRES_REMAPPING}`,
    }),
    errors: (count: number) => ({
      name: t('pulse.onboarding.monitoring.errors', { count: count }),
      color: 'status.error',
      searchParams: `status=${AssetMapping.status.MISSING}`,
    }),
  }

  const aggregate = useMemo(() => {
    if (!assets?.items) return undefined
    const aggregate = {
      unmapped: 0,
      remapping: 0,
      errors: 0,
    }

    return assets.items.reduce((acc, asset) => {
      if (asset.mapping.status === AssetMapping.status.UNMAPPED) acc.unmapped++
      if (asset.mapping.status === AssetMapping.status.REQUIRES_REMAPPING) acc.remapping++
      if (asset.mapping.status === AssetMapping.status.MISSING) acc.errors++
      return acc
    }, aggregate)
  }, [assets?.items])

  const isTaskClear = aggregate?.unmapped === 0 && aggregate?.remapping === 0 && aggregate?.errors === 0

  if (isAssetLoading) return <LoaderSpinner />
  if (error) return <ErrorMessage message={error.message} />

  return (
    <List data-testid="asset-monitoring-onboarding-todo">
      {isTaskClear && (
        <ListItem display="flex" gap={4} alignItems="center" mb={1}>
          <Badge colorScheme="green">
            <CheckIcon />
          </Badge>
          <Text data-testid="asset-monitoring-todo-text">{t('pulse.onboarding.monitoring.allClear')}</Text>
        </ListItem>
      )}
      {!isTaskClear &&
        Object.entries(aggregate || {}).map(([key, value]) => {
          const category = categories[key](value)
          return (
            <ListItem key={key} display="flex" gap={4} alignItems="center" mb={1}>
              <Badge
                data-testid="asset-monitoring-todo-count"
                colorScheme={value ? category.color || 'gray' : undefined}
              >
                {value}
              </Badge>
              {Boolean(value) && (
                <Button
                  data-testid="asset-monitoring-todo-link"
                  variant="link"
                  as={RouterLink}
                  to={`/pulse-assets${category.searchParams ? '?' + category.searchParams : ''}`}
                >
                  {category.name}
                </Button>
              )}
              {!value && <Text data-testid="asset-monitoring-todo-text">{category.name}</Text>}
            </ListItem>
          )
        })}
    </List>
  )
}

export default AssetMonitoringOnboardingTask
