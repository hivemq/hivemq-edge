import { FC } from 'react'
import { Box, SkeletonCircle, SkeletonText } from '@chakra-ui/react'

const Skeleton: FC = () => {
  return (
    <Box padding={6} boxShadow="md" bg="white">
      <SkeletonCircle size="10" />
      <SkeletonText mt="4" noOfLines={4} spacing="4" skeletonHeight="2" />
    </Box>
  )
}

export default Skeleton
