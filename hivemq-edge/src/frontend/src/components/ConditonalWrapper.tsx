import React, { FC } from 'react'

interface ConditionalWrapperProps {
  children: React.ReactElement
  condition: boolean
  wrapper: (children: React.ReactElement) => JSX.Element
}

export const ConditionalWrapper: FC<ConditionalWrapperProps> = ({ condition, wrapper, children }) =>
  condition ? wrapper(children) : children
