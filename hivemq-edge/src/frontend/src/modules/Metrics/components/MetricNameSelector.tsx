import { FC, useEffect, useMemo } from 'react'
import { Select } from 'chakra-react-select'
import { Controller, SubmitHandler, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Box, Button, Flex, FormControl, FormLabel } from '@chakra-ui/react'
import { BiAddToQueue } from 'react-icons/bi'

import { useGetMetrics } from '@/api/hooks/useGetMetrics/useGetMetrics.tsx'
import { extractMetricInfo } from '../utils/metrics-name.utils.ts'

interface MetricNameOption {
  label: string
  value: string
  isDisabled?: boolean
}

interface MetricNameSelectorForm {
  selectedTopic: MetricNameOption
}

interface MetricNameSelectorProps {
  onSubmit: SubmitHandler<MetricNameSelectorForm>
  selectedMetrics: string[]
  filter: string
}

const MetricNameSelector: FC<MetricNameSelectorProps> = ({ onSubmit, filter, selectedMetrics }) => {
  const { t } = useTranslation()
  const { data } = useGetMetrics()
  const {
    handleSubmit,
    control,
    reset,
    formState: { isValid, isSubmitted },
  } = useForm<MetricNameSelectorForm>()

  const sortedItems: MetricNameOption[] = useMemo(() => {
    if (!data || !data.items) return []

    return data.items
      .filter((e) => e.name && e.name.includes(filter))
      .map((e) => {
        const { device, suffix } = extractMetricInfo(e.name as string)
        return {
          label: t(`metrics.${device}.${suffix}`),
          value: e.name as string,
          isDisabled: selectedMetrics?.includes(e.name as string),
        }
      })
      .sort((a, b) => a.label.localeCompare(b.label))
  }, [data, filter, selectedMetrics, t])

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
      <FormControl>
        <FormLabel htmlFor={'tlsConfiguration.protocols'}>{t('welcome.metrics.select')}</FormLabel>
        <Flex gap={2}>
          <Box flex={1}>
            <Controller
              name={'selectedTopic'}
              control={control}
              rules={{
                required: true,
              }}
              render={({ field }) => {
                const { value, onChange, ...rest } = field
                return (
                  <Select
                    {...rest}
                    inputId={'tlsConfiguration.protocols'}
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
          </Box>

          <Button isDisabled={!isValid} rightIcon={<BiAddToQueue />} type="submit" form="namespace-form">
            {t('welcome.metrics.display')}
          </Button>
        </Flex>
      </FormControl>
    </form>
  )
}

export default MetricNameSelector
