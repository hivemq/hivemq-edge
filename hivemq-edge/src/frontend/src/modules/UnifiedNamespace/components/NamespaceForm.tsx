import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'
import {
  Checkbox,
  Flex,
  Text,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Grid,
  Input,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { $ISA95ApiBean, ISA95ApiBean } from '@/api/__generated__'
import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'

import NamespaceDisplay from './NamespaceDisplay.tsx'
import { NAMESPACE_SEPARATOR } from '../namespace-utils.ts'

interface NamespaceFormProps {
  defaultValues: ISA95ApiBean
  onSubmit: SubmitHandler<ISA95ApiBean>
}

const FormControlSeparator = () => (
  <Text pt={'2.33rem'} textAlign={'center'}>
    {NAMESPACE_SEPARATOR}
  </Text>
)

const NamespaceForm: FC<NamespaceFormProps> = ({ defaultValues, onSubmit }) => {
  const { t } = useTranslation()
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ISA95ApiBean>({
    defaultValues: { ...defaultValues, enabled: true },
  })
  const getRulesForProperty = useValidationRules()

  const preview = watch()

  return (
    <form id="namespace-form" onSubmit={handleSubmit(onSubmit)}>
      <Flex flexDirection={'column'} gap={6}>
        <FormControl>
          <FormLabel htmlFor={'unifiedNamespace-preview'}>{t('unifiedNamespace.preview.label')}</FormLabel>
          <NamespaceDisplay namespace={preview} fontSize={'sm'} />
        </FormControl>

        <Flex flexDirection={'column'} gap={4}>
          <Grid templateColumns="repeat(2, 1fr 20px)" gap={6}>
            <FormControl isInvalid={!!errors.enterprise}>
              <FormLabel htmlFor={'unifiedNamespace-enterprise'}>{t('unifiedNamespace.enterprise.label')}</FormLabel>
              <Input
                id={'unifiedNamespace-enterprise'}
                {...register('enterprise', {
                  ...getRulesForProperty($ISA95ApiBean.properties.enterprise),
                })}
              />
              {!errors.enterprise && <FormHelperText>{t('unifiedNamespace.enterprise.helper')}</FormHelperText>}
              <FormErrorMessage>{errors.enterprise && errors.enterprise.message}</FormErrorMessage>
            </FormControl>

            <FormControlSeparator />

            <FormControl>
              <FormLabel htmlFor={'unifiedNamespace-site'}>{t('unifiedNamespace.site.label')}</FormLabel>
              <Input
                id={'unifiedNamespace-site'}
                {...register('site', {
                  ...getRulesForProperty($ISA95ApiBean.properties.site),
                })}
              />
              <FormHelperText>{t('unifiedNamespace.site.helper')}</FormHelperText>
            </FormControl>

            <FormControlSeparator />

            <FormControl>
              <FormLabel htmlFor={'unifiedNamespace-area'}>{t('unifiedNamespace.area.label')}</FormLabel>
              <Input
                id={'unifiedNamespace-area'}
                {...register('area', {
                  ...getRulesForProperty($ISA95ApiBean.properties.area),
                })}
              />
              <FormHelperText>{t('unifiedNamespace.area.helper')}</FormHelperText>
            </FormControl>

            <FormControlSeparator />

            <FormControl>
              <FormLabel htmlFor={'unifiedNamespace-productionLine'}>
                {t('unifiedNamespace.productionLine.label')}
              </FormLabel>
              <Input
                id={'unifiedNamespace-productionLine'}
                {...register('productionLine', {
                  ...getRulesForProperty($ISA95ApiBean.properties.productionLine),
                })}
              />
              <FormHelperText>{t('unifiedNamespace.productionLine.helper')}</FormHelperText>
            </FormControl>

            <FormControlSeparator />

            <FormControl>
              <FormLabel htmlFor={'unifiedNamespace-workCell'}>{t('unifiedNamespace.workCell.label')}</FormLabel>
              <Input
                id={'unifiedNamespace-workCell'}
                {...register('workCell', {
                  ...getRulesForProperty($ISA95ApiBean.properties.workCell),
                })}
              />
              <FormHelperText>{t('unifiedNamespace.workCell.helper')}</FormHelperText>
            </FormControl>
          </Grid>
        </Flex>

        <FormControl as="fieldset" isInvalid={!!errors.prefixAllTopics}>
          <FormLabel as="legend">{t('unifiedNamespace.options.legend')}</FormLabel>
          <Checkbox
            data-testid="unifiedNamespace-prefixAllTopics"
            id={'unifiedNamespace-prefixAllTopics'}
            {...register('prefixAllTopics', {
              ...getRulesForProperty($ISA95ApiBean.properties.prefixAllTopics),
            })}
          >
            {t('unifiedNamespace.prefixAllTopics.label')}
          </Checkbox>
          <FormHelperText> {t('unifiedNamespace.prefixAllTopics.helper')}</FormHelperText>
          <FormErrorMessage>{errors.prefixAllTopics && errors.prefixAllTopics.message}</FormErrorMessage>
        </FormControl>
      </Flex>
    </form>
  )
}

export default NamespaceForm
