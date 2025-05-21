import IconButton from '@/components/Chakra/IconButton.tsx'
import { Icon } from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { LuChevronRight } from 'react-icons/lu'

interface ExpandVersionButtonProps {
  onClick: () => void
  isExpanded: boolean
}

export const ExpandVersionButton: FC<ExpandVersionButtonProps> = ({ onClick, isExpanded }) => {
  const { t } = useTranslation('datahub')

  return (
    <IconButton
      data-testid="list-action-collapse"
      onClick={onClick}
      size="sm"
      variant="ghost"
      colorScheme="gray"
      aria-label={isExpanded ? t('Listings.action.collapse') : t('Listings.action.expand')}
      icon={<Icon as={LuChevronRight} fontSize="1rem" transform={isExpanded ? 'rotate(90deg)' : undefined} />}
    />
  )
}
