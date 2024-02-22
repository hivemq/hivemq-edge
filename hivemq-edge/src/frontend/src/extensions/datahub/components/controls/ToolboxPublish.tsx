import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { UseMutateAsyncFunction } from '@tanstack/react-query'

import { Box, Button, HStack, Icon, Stack, useToast, UseToastOptions } from '@chakra-ui/react'
import { MdPublishedWithChanges } from 'react-icons/md'

import { DataPolicy, Schema, type Script } from '@/api/__generated__'

import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { DataHubNodeType, DryRunResults } from '@datahub/types.ts'
import { useCreateSchema } from '@datahub/api/hooks/DataHubSchemasService/useCreateSchema.tsx'
import { useCreateScript } from '@datahub/api/hooks/DataHubScriptsService/useCreateScript.tsx'
import { useCreateDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useCreateDataPolicy.tsx'

const resourceReducer =
  <T extends { id: string }>(type: DataHubNodeType) =>
  (accumulator: T[], result: DryRunResults<T, never>) => {
    if (result.node.type !== type) return accumulator
    if (!result.data) return accumulator
    const { id } = result.data
    if (!id) return accumulator

    const allIds = accumulator.map((resource) => resource.id)
    if (allIds.includes(id)) return accumulator

    accumulator.push(result.data)
    return accumulator
  }

interface Mutate<T> {
  type: DataHubNodeType
  payload: T
  mutation: UseMutateAsyncFunction<T, unknown, T>
}

export const ToolboxPublish: FC = () => {
  const { t } = useTranslation('datahub')
  const { report } = usePolicyChecksStore()
  const createSchema = useCreateSchema()
  const createScript = useCreateScript()
  const createPolicy = useCreateDataPolicy()
  const toast = useToast()

  const isValid = report?.every((e) => !e.error)

  const handlePublish = () => {
    if (!report) return

    const payload = [...report].pop()
    if (payload && payload.node.type === DataHubNodeType.DATA_POLICY) {
      const { data, resources } = payload

      const allSchemas =
        resources?.reduce(resourceReducer<Schema>(DataHubNodeType.SCHEMA), []).map<Mutate<Schema>>((e) => ({
          type: DataHubNodeType.SCHEMA,
          payload: e,
          mutation: createSchema.mutateAsync,
        })) || []

      const allScripts =
        resources?.reduce(resourceReducer<Script>(DataHubNodeType.FUNCTION), []).map<Mutate<Script>>((e) => ({
          type: DataHubNodeType.FUNCTION,
          payload: e,
          mutation: createScript.mutateAsync,
        })) || []

      const policy: Mutate<DataPolicy> = {
        type: DataHubNodeType.DATA_POLICY,
        payload: data as DataPolicy,
        mutation: createPolicy.mutateAsync,
      }

      const allMutations = [...allSchemas, ...allScripts, policy]

      const toastOption: UseToastOptions = {
        duration: 9000,
        isClosable: true,
        position: 'top-right',
      }

      allMutations.map((request) =>
        request
          // @ts-ignore Check the type mismatch
          .mutation(request.payload)
          .then(() =>
            toast({
              ...toastOption,
              title: `${request.type} created.`,
              description: t('error.publish.description', { source: request.type }),
              status: 'success',
            })
          )
          .catch((err) =>
            toast({
              ...toastOption,
              title: t('error.publish.error', { source: request.type }),
              description: err.toString(),
              status: 'error',
            })
          )
      )
    }
    return undefined
  }

  return (
    <Stack maxW={500}>
      <HStack>
        <Box>
          <Button
            leftIcon={<Icon as={MdPublishedWithChanges} boxSize="24px" />}
            onClick={handlePublish}
            isDisabled={!report || !isValid}
            isLoading={createSchema.isLoading}
          >
            {t('workspace.toolbar.policy.publish')}
          </Button>
        </Box>
      </HStack>
    </Stack>
  )
}
