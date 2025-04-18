import { createContext } from 'react'
import { type AccessibleDraggableProps } from './type'

export const AccessibleDraggableContext = createContext<AccessibleDraggableProps | null>(null)
