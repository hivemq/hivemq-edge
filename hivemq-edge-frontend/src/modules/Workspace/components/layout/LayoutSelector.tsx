/**
 * Layout Algorithm Selector Component
 *
 * Dropdown to select which layout algorithm to use.
 */

import { type FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Select, Tooltip } from '@chakra-ui/react'
import config from '@/config'
import { useLayoutEngine } from '../../hooks/useLayoutEngine.ts'
import type { LayoutType } from '../../types/layout.ts'

const LayoutSelector: FC = () => {
  const { t } = useTranslation()
  const { currentAlgorithm, setAlgorithm, availableAlgorithms } = useLayoutEngine()

  if (!config.features.WORKSPACE_AUTO_LAYOUT) {
    return null
  }

  const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const newAlgorithm = event.target.value as LayoutType
    setAlgorithm(newAlgorithm)
  }

  return (
    <Tooltip label={t('workspace.autoLayout.selector.tooltip')} placement="bottom">
      <Select
        data-testid="workspace-layout-selector"
        aria-label={t('workspace.autoLayout.selector.ariaLabel')}
        size="sm"
        value={currentAlgorithm}
        onChange={handleChange}
        maxWidth="200px"
        bg="white"
        _dark={{ bg: 'gray.700' }}
      >
        {availableAlgorithms.map((algo) => (
          <option key={algo.type} value={algo.type}>
            {algo.name}
          </option>
        ))}
      </Select>
    </Tooltip>
  )
}

export default LayoutSelector
