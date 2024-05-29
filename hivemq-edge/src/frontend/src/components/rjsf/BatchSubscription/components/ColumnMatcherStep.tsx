import { FC, useMemo } from 'react'
import { StepRendererProps } from '@/components/rjsf/BatchSubscription/types.ts'
import { Box, HStack, VStack } from '@chakra-ui/react'
import { Select } from 'chakra-react-select'
import { LuChevronsRight } from 'react-icons/lu'
import { JSONSchema7 } from 'json-schema'

interface ColumnOption {
  value: string | number
  label: string
  type?: string
}

const ColumnMatcherStep: FC<StepRendererProps> = ({ store }) => {
  const { schema, worksheet } = store

  const subscriptions = useMemo<ColumnOption[]>(() => {
    const { required, properties } = schema.items as JSONSchema7

    //  TODO[NVL] If not required properties, do we "force" some (assuming a mqtt-topic for example)
    return required
      ? required.map((e) => {
          const sss = properties?.[e] as JSONSchema7 | undefined
          return { value: e, label: sss?.title || e }
        })
      : []
  }, [schema.items])

  const columns = useMemo<ColumnOption[]>(() => {
    const header = worksheet?.[0]
    //  TODO[NVL] Throw an error and handle failure
    if (!header) return []
    //  TODO[NVL] deal with duplicate headers (seems to be adding _number)
    //  TODO[NVL] deal with no header!
    return Object.keys(header).map((e) => ({ value: e, label: e }))
  }, [worksheet])

  return (
    <VStack>
      {subscriptions.map((subscription) => (
        <HStack w="100%" key={subscription.value}>
          <Box w="100%">
            <Select<ColumnOption> options={columns} />
          </Box>
          <Box>
            <LuChevronsRight />
          </Box>
          <Box w="100%">
            <Select
              options={[subscription]}
              defaultValue={subscription}
              isReadOnly
              components={{ DropdownIndicator: undefined }}
            />
          </Box>
        </HStack>
      ))}
    </VStack>
  )
}

export default ColumnMatcherStep
