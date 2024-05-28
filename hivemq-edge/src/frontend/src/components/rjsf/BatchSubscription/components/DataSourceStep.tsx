import { FC } from 'react'
import { VStack } from '@chakra-ui/react'

const getDropZoneBorder = (color: string) => {
  return {
    bgGradient: `repeating-linear(0deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(90deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(180deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(270deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px)`,
    backgroundSize: '2px 100%, 100% 2px, 2px 100% , 100% 2px',
    backgroundPosition: '0 0, 0 0, 100% 0, 0 100%',
    backgroundRepeat: 'no-repeat',
    borderRadius: '4px',
  }
}

const DataSourceStep: FC = () => {
  return (
    <VStack
      color="red"
      {...getDropZoneBorder('blue.500')}
      minHeight="calc(450px - 2rem)"
      display="flex"
      justifyContent="center"
      alignItems="center"
    ></VStack>
  )
}

export default DataSourceStep
