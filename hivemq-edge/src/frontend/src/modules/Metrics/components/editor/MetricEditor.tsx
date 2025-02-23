import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import type { OptionsOrGroups, GroupBase } from 'chakra-react-select'
import { Select } from 'chakra-react-select'
import type { SubmitHandler } from 'react-hook-form'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button, FormControl, FormLabel, VStack } from '@chakra-ui/react'
import { BiAddToQueue } from 'react-icons/bi'

import { useGetMetrics } from '@/api/hooks/useGetMetrics/useGetMetrics.ts'

import { extractMetricInfo } from '../../utils/metrics-name.utils.ts'
import type { ChartTypeOption, MetricDefinition, MetricNameOption, MetricsFilter } from '../../types.ts'
import { ChartType } from '../../types.ts'

interface MetricEditorProps {
  onSubmit: SubmitHandler<MetricDefinition>
  selectedMetrics: string[]
  selectedChart?: ChartType
  filters: MetricsFilter[]
}

const chartTypeOptions: ChartTypeOption[] = [
  { value: ChartType.BAR_CHART, label: 'Bar Chart' },
  { value: ChartType.LINE_CHART, label: 'Line chart' },
  { value: ChartType.SAMPLE, label: 'Stat' },
]

const MetricEditor: FC<MetricEditorProps> = ({ onSubmit, filters, selectedMetrics, selectedChart }) => {
  const { t } = useTranslation()
  const { data } = useGetMetrics()
  const {
    handleSubmit,
    control,
    reset,
    formState: { isValid, isSubmitted },
  } = useForm<MetricDefinition>({
    // defaultValues: { selectedChart: { value: selectedChart || ChartType.SAMPLE, label: 'ddd' } },
  })

  const sortedItems: OptionsOrGroups<MetricNameOption, GroupBase<MetricNameOption>> = useMemo(() => {
    if (!data || !data.items) return []
    const { items } = data

    const groupedMetrics = filters
      .map((filter) => {
        const metrics: MetricNameOption[] = items
          .filter((metric) => {
            const structure = extractMetricInfo(metric.name as string)
            return filter.id === structure.id
          })
          .map((metric) => {
            const { device, suffix } = extractMetricInfo(metric.name as string)
            return {
              label: t(`metrics.${device}.${suffix}`),
              value: metric.name as string,
              isDisabled: selectedMetrics?.includes(metric.name as string),
            }
          })
          .sort((a, b) => a.label.localeCompare(b.label))

        return {
          label: filter.id,
          options: metrics,
        }
      })
      .sort((a, b) => a.label.localeCompare(b.label))

    return groupedMetrics.length === 1 ? groupedMetrics[0].options : groupedMetrics
  }, [data, filters, selectedMetrics, t])

  useEffect(() => {
    if (isSubmitted) {
      reset()
    }
  }, [isSubmitted, reset])

  return (
    <form
      id="namespace-form"
      onSubmit={handleSubmit(onSubmit)}
      style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
    >
      <VStack gap={2} alignItems="flex-end">
        <FormControl>
          <FormLabel htmlFor="metrics-select">{t('metrics.editor.select-metric')}</FormLabel>

          <Controller
            name="selectedTopic"
            control={control}
            rules={{
              required: true,
            }}
            render={({ field }) => {
              const { value, onChange, ...rest } = field
              return (
                <Select
                  {...rest}
                  menuPortalTarget={document.body}
                  chakraStyles={{
                    option: (provided) => ({ ...provided, paddingLeft: '24px' }),
                  }}
                  styles={{
                    menuPortal: (provided) => ({ ...provided, zIndex: 1401 }),
                  }}
                  id="metrics-select-container"
                  inputId="metrics-select"
                  value={value || null}
                  onChange={(values) => onChange(values)}
                  options={sortedItems}
                  isClearable={true}
                  isMulti={false}
                  isSearchable={true}
                  components={{
                    DropdownIndicator: null,
                  }}
                />
              )
            }}
          />
        </FormControl>
        {!selectedChart && (
          <FormControl>
            <FormLabel htmlFor="chart-select">{t('metrics.editor.select-chart')}</FormLabel>

            <Controller
              name="selectedChart"
              control={control}
              rules={{
                required: true,
              }}
              render={({ field }) => {
                const { value, onChange, ...rest } = field
                return (
                  <Select
                    {...rest}
                    menuPortalTarget={document.body}
                    styles={{
                      menuPortal: (provided) => ({ ...provided, zIndex: 1401 }),
                    }}
                    id="chart-select-container"
                    inputId="chart-select"
                    value={value || null}
                    onChange={(values) => onChange(values)}
                    options={chartTypeOptions}
                    isClearable={true}
                    isMulti={false}
                    isSearchable={false}
                    components={{
                      DropdownIndicator: null,
                    }}
                  />
                )
              }}
            />
          </FormControl>
        )}
        <Button isDisabled={!isValid} rightIcon={<BiAddToQueue />} type="submit" form="namespace-form">
          {t('metrics.editor.add')}
        </Button>
      </VStack>
    </form>
  )
}

export default MetricEditor
