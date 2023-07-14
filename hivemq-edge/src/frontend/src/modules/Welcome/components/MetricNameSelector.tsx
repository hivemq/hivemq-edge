import { FC, useEffect, useMemo } from 'react'
import { Select } from 'chakra-react-select'
import { Box, Button, Flex, FormControl, FormLabel } from '@chakra-ui/react'
import { Controller, SubmitHandler, useForm } from 'react-hook-form'

import { useGetMetrics } from '@/api/hooks/useGetMetrics/useGetMetrics.tsx'
import { BiAddToQueue } from 'react-icons/bi'

interface MetricNameSelectorForm {
  myTopic: string
}

interface MetricNameSelectorProps {
  onSubmit: SubmitHandler<MetricNameSelectorForm>
  selectedMetrics: string[]
}

const MetricNameSelector: FC<MetricNameSelectorProps> = ({ onSubmit }) => {
  const { data } = useGetMetrics()
  const {
    handleSubmit,
    control,
    reset,
    formState: { isValid, isSubmitted },
  } = useForm<MetricNameSelectorForm>()

  const sortedItems: string[] = useMemo(() => {
    if (!data?.items) return []
    return (
      data.items.sort((a, b) => (a.name as string).localeCompare(b.name as string)).map((e) => e.name as string) || ''
    )
  }, [data])

  useEffect(() => {
    if (isSubmitted) reset()
  }, [isSubmitted, reset])

  return (
    <form
      id="namespace-form"
      onSubmit={handleSubmit(onSubmit)}
      style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
    >
      <FormControl>
        <FormLabel htmlFor={'tlsConfiguration.protocols'}>Select a metric to display</FormLabel>
        <Flex>
          <Box flex={1}>
            <Controller
              name={'myTopic'}
              control={control}
              rules={{
                required: true,
              }}
              render={({ field }) => {
                const { value, onChange, ...rest } = field
                return (
                  <Select
                    {...rest}
                    value={{ label: value, value: value }}
                    inputId={'tlsConfiguration.protocols'}
                    onChange={(values) => onChange(values?.value)}
                    options={sortedItems.map((e) => ({ label: e, value: e }))}
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
            Add to dashboard
          </Button>
        </Flex>
      </FormControl>
    </form>
  )
}

export default MetricNameSelector
