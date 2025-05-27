import type { CSSProperties, FC } from 'react'
import { MdPolicy } from 'react-icons/md'
import { ButtonGroup, Icon, Text, useColorMode } from '@chakra-ui/react'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { useTranslation } from 'react-i18next'

interface DataPolicyIconsProps {
  policyRoutes: string[]
  style?: CSSProperties
  onClickPolicy?: (route: string) => void
  onClickAll?: () => void
}

const MAX_LINKS = 2

const DataPolicyEdgeCTA: FC<DataPolicyIconsProps> = ({ policyRoutes, style, onClickPolicy, onClickAll }) => {
  const { t } = useTranslation()
  const { colorMode } = useColorMode()

  const showNode = policyRoutes.slice(0, MAX_LINKS)
  const moreElements = policyRoutes.length - MAX_LINKS

  if (policyRoutes.length === 0) return null

  return (
    <ButtonGroup isAttached data-testid="reactFlow-edge-policy-group">
      {showNode.map((route) => (
        <IconButton
          data-testid="policy-panel-trigger"
          key={route}
          aria-label={t('workspace.datahub.aria-label')}
          variant={colorMode === 'light' ? 'outline' : 'solid'}
          icon={<Icon as={MdPolicy} boxSize={6} />}
          backgroundColor={colorMode === 'light' ? 'white' : 'gray.700'}
          color={style?.stroke}
          onClick={() => onClickPolicy?.(route)}
          borderRadius={25}
        />
      ))}
      {moreElements > 0 && (
        <IconButton
          data-testid="policy-panel-list"
          aria-label={t('workspace.datahub.list')}
          variant={colorMode === 'light' ? 'outline' : 'solid'}
          icon={<Text>+{moreElements}</Text>}
          backgroundColor={colorMode === 'light' ? 'white' : 'gray.700'}
          color={style?.stroke}
          onClick={onClickAll}
          borderRadius={25}
        />
      )}
    </ButtonGroup>
  )
}

export default DataPolicyEdgeCTA
