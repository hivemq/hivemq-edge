import type { FC, PropsWithChildren } from 'react'
import { useLocation } from 'react-router-dom'
import { Card, CardBody, CardHeader, Code } from '@chakra-ui/react'

export const WrapperTestRoute: FC<PropsWithChildren> = ({ children }) => {
  const { pathname, state } = useLocation()

  return (
    <>
      {children}
      <Card mt={50} variant="filled" size="sm">
        <CardHeader>Testing Dashboard</CardHeader>
        <CardBody data-testid="test-pathname" as={Code}>
          {pathname}
        </CardBody>
        {state && (
          <CardBody data-testid="test-state" as={Code}>
            {JSON.stringify(state)}
          </CardBody>
        )}
      </Card>
    </>
  )
}
