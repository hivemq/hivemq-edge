import { FC } from 'react'
import { FormControl, Text } from '@chakra-ui/react'
import { Capability } from '@/api/__generated__'

const PersistencePanel: FC<{ hasPersistence: Capability }> = ({ hasPersistence }) => {
  return (
    <FormControl variant={'hivemq'} flexGrow={1} display={'flex'} flexDirection={'column'} gap={4} as={'fieldset'}>
      <Text>{hasPersistence.description}</Text>
    </FormControl>
  )
}

export default PersistencePanel
