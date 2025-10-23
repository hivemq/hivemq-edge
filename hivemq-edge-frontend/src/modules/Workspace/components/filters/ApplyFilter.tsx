import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, HStack } from '@chakra-ui/react'

interface ApplyFilterProps {
  onSubmit?: () => void
  onClear?: () => void
}

const ApplyFilter: FC<ApplyFilterProps> = ({ onSubmit, onClear }) => {
  const { t } = useTranslation()

  return (
    <HStack>
      <Button onClick={onSubmit} data-testid="filter-apply">
        {t('workspace.searchToolbox.action.apply')}
      </Button>
      <Button onClick={onClear} data-testid="filter-clearAll">
        {t('workspace.searchToolbox.action.clear')}
      </Button>
    </HStack>
  )
}

export default ApplyFilter
