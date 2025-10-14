import type { FilterOperationOption } from '@/modules/Workspace/components/filters/types.ts'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardBody,
  FormControl,
  FormHelperText,
  FormLabel,
  Radio,
  RadioGroup,
  Stack,
  Switch,
  VStack,
} from '@chakra-ui/react'

// TODO[NVL] AND is not yet supported
const FILTER_JOIN_OPTIONS = ['OR' /* 'AND' */] as const
type FilterJoinOptions = (typeof FILTER_JOIN_OPTIONS)[number]

interface FilterDynamicProps {
  value?: FilterOperationOption
  onChange?: (
    prop: keyof FilterOperationOption,
    value: FilterOperationOption['joinOperator'] | FilterOperationOption['isLiveUpdate']
  ) => void
}

const OptionsFilter: FC<FilterDynamicProps> = ({ value, onChange }) => {
  const { t } = useTranslation()

  return (
    <Card
      size="sm"
      alignContent="space-between"
      width="-webkit-fill-available"
      data-testid="workspace-filter-options-container"
    >
      <CardBody gap={3} as={VStack}>
        <FormControl variant="horizontal">
          <FormLabel fontSize="sm" htmlFor="workspace-filter-join">
            {t('workspace.searchToolbox.join.label')}
          </FormLabel>
          <VStack alignItems="flex-start">
            <RadioGroup
              onChange={(value: FilterJoinOptions) => onChange?.('joinOperator', value)}
              defaultValue={value?.joinOperator || 'OR'}
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
            <Switch
              id="workspace-filter-dynamic-update"
              onChange={(e) => onChange?.('isLiveUpdate', e.target.checked)}
              isChecked={value?.isLiveUpdate || false}
            />
            <FormHelperText>{t('workspace.searchToolbox.liveUpdate.helper')}</FormHelperText>
          </VStack>
        </FormControl>
      </CardBody>
    </Card>
  )
}

export default OptionsFilter
