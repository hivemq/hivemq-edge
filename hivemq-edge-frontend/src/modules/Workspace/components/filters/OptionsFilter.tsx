import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormHelperText, FormLabel, Radio, RadioGroup, Stack, Switch, VStack } from '@chakra-ui/react'

const FILTER_JOIN_OPTIONS = ['OR', 'AND'] as const
type FilterJoinOptions = (typeof FILTER_JOIN_OPTIONS)[number]

interface FilterDynamicProps {
  onChangeDynamic?: (value: boolean) => void
  onChangeJoin?: (value: FilterJoinOptions) => void
}

const OptionsFilter: FC<FilterDynamicProps> = ({ onChangeDynamic, onChangeJoin }) => {
  const { t } = useTranslation()

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChangeDynamic?.(event.target.checked)
  }

  return (
    <>
      <FormControl variant="horizontal">
        <FormLabel fontSize="sm" htmlFor="workspace-filter-join">
          {t('workspace.searchToolbox.join.label')}
        </FormLabel>
        <VStack alignItems="flex-start">
          <RadioGroup
            onChange={(value: FilterJoinOptions) => onChangeJoin?.(value)}
            defaultValue={FILTER_JOIN_OPTIONS[0]}
            id="workspace-filter-join"
          >
            <Stack direction="row">
              {FILTER_JOIN_OPTIONS.map((option) => (
                <Radio key={option} value={option}>
                  {t('workspace.searchToolbox.join.option', { context: option })}
                </Radio>
              ))}
            </Stack>
          </RadioGroup>
          <FormHelperText>{t('workspace.searchToolbox.join.helper')}</FormHelperText>
        </VStack>
      </FormControl>
      <FormControl variant="horizontal">
        <FormLabel fontSize="sm" htmlFor="workspace-filter-dynamic-update">
          {t('workspace.searchToolbox.liveUpdate.label')}
        </FormLabel>
        <VStack alignItems="flex-start">
          <Switch id="workspace-filter-dynamic-update" onChange={handleChange} />
          <FormHelperText>{t('workspace.searchToolbox.liveUpdate.helper')}</FormHelperText>
        </VStack>
      </FormControl>
    </>
  )
}

export default OptionsFilter
