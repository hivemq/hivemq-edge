import { RangeOption } from '@/components/DateTime/types.ts'
import { Box, Button } from '@chakra-ui/react'
import { FC } from 'react'

interface OptionCommandProps {
  data: RangeOption
}

const OptionCommand: FC<OptionCommandProps> = ({ data }) => {
  return (
    <Box w={'100%'}>
      <Button variant={'ghost'} size={'sm'} isDisabled>
        {data.label}
      </Button>
    </Box>
  )
}

export default OptionCommand
