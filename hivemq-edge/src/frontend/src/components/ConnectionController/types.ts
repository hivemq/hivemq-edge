import { StatusTransitionCommand } from '@/api/__generated__'

export interface ConnectionElementProps {
  id: string
  isRunning: boolean
  isLoading?: boolean
  onChangeStatus?: (id: string, status: StatusTransitionCommand.command) => void
}
