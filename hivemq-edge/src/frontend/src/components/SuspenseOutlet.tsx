import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import { AbsoluteCenter } from '@chakra-ui/react'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

export const SuspenseFallback = () => {
  return (
    <AbsoluteCenter axis="both">
      <LoaderSpinner />
    </AbsoluteCenter>
  )
}

const SuspenseOutlet = () => {
  return (
    <Suspense fallback={<SuspenseFallback />}>
      <Outlet />
    </Suspense>
  )
}

export default SuspenseOutlet
