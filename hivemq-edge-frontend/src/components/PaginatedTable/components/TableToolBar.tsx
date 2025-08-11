import { useTranslation } from 'react-i18next'
import { HStack } from '@chakra-ui/react'

interface TableToolBarProps {
  leftControls?: React.ReactNode
  rightControls?: React.ReactNode
}

const TableToolBar = ({ leftControls, rightControls }: TableToolBarProps) => {
  const { t } = useTranslation('components')

  if (!leftControls && !rightControls) return null
  return (
    <HStack role="group" aria-label={t('TableToolBar.aria-label')} gap={8} mb={4} justifyContent="space-between#">
      {leftControls}
      {rightControls}
    </HStack>
  )
}

export default TableToolBar
