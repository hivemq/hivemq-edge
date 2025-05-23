import type { FC } from 'react'
import { Box, Button } from '@chakra-ui/react'
import type { RangeOption } from '@/components/DateTime/types.ts'

interface OptionCommandProps {
  data: RangeOption
}

const OptionCommand: FC<OptionCommandProps> = ({ data }) => {
  return (
    <Box w="100%">
      <Button variant="ghost" size="sm" isDisabled>
        {data.label}
      </Button>
    </Box>
  )
}

export default OptionCommand
