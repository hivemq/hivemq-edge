import type { SystemStyleObject } from '@chakra-ui/react'

export const fff: SystemStyleObject = {}

export const filterContainerStyle = (style: SystemStyleObject) => ({
  ...style,
  // backgroundColor: 'red',
  minWidth: 'var(--chakra-sizes-3xs)',
  maxWidth: 'var(--chakra-sizes-xs)',
})
