import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormHelperText, FormLabel, Radio, RadioGroup, Stack, Switch, VStack } from '@chakra-ui/react'

interface FilterDynamicProps {
  onChangeDynamic?: (value: boolean) => void
  onChangeJoin?: (value: 'AND' | 'OR') => void
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
          {t('Join filters using')}
        </FormLabel>
        <RadioGroup
          onChange={(value: 'AND' | 'OR') => onChangeJoin?.(value)}
          defaultValue="OR"
          id="workspace-filter-join"
        >
          <Stack direction="row">
            <Radio value="AND">AND</Radio>
            <Radio value="OR">OR</Radio>
          </Stack>
        </RadioGroup>
      </FormControl>
      <FormControl variant="horizontal">
        <FormLabel fontSize="sm" htmlFor="workspace-filter-dynamic-update">
          {t('Live updating')}
        </FormLabel>
        <VStack alignItems="flex-start">
          <Switch id="workspace-filter-dynamic-update" onChange={handleChange} />
          <FormHelperText>Turn on if you wish the filters to be automatically updated</FormHelperText>
        </VStack>
      </FormControl>
    </>
  )
}

export default OptionsFilter
