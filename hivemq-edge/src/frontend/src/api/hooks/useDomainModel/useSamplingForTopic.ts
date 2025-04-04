import { useStartSamplingForTopic } from '@/api/hooks/useDomainModel/useStartSamplingForTopic.ts'
import { useGetSchemaForTopic } from '@/api/hooks/useDomainModel/useGetSchemaForTopic.ts'
import { useGetSamplesForTopic } from '@/api/hooks/useDomainModel/useGetSamplesForTopic.ts'
import { useEffect, useMemo } from 'react'
import { reducerSchemaExamples } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

export const useSamplingForTopic = (topic: string) => {
  const {
    isSuccess: isMutateSuccess,
    isPending,
    isError: isMutateError,
    error: mutateError,
    mutate,
  } = useStartSamplingForTopic()
  const {
    data,
    isLoading: isSchemaLoading,
    isError: isSchemaError,
    error: schemaError,
    isSuccess: isSchemaSuccess,
  } = useGetSchemaForTopic(topic, isMutateSuccess)
  const {
    data: samples,
    error: sampleError,
    isLoading: isSampleLoading,
    refetch,
    isError: isSampleError,
  } = useGetSamplesForTopic(topic, isSchemaSuccess)

  useEffect(() => {
    mutate(topic)
  }, [mutate, topic])

  const schema = useMemo(() => {
    if (!data) return undefined
    let schema = data
    if (samples) {
      const payloadSample = samples.items[0]
      if (payloadSample) {
        const { payload } = payloadSample
        schema = reducerSchemaExamples(data, JSON.parse(atob(payload)))
      }
    }
    return schema
  }, [data, samples])

  const isLoading = isSchemaLoading || isSampleLoading || isPending
  const isError = isSchemaError || isSampleError || isMutateError
  const error = schemaError || sampleError || mutateError
  const isSuccess = isMutateSuccess || isSchemaSuccess

  return { data, schema, isLoading, isSuccess, refetch, isError, error }
}
