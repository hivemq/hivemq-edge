import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'
import { ISA95ApiBean } from '@/api/__generated__'
import {
  Box,
  Button,
  Checkbox,
  Flex,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Grid,
  Input,
  Switch,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

interface NamespaceFormProps {
  defaultValues: ISA95ApiBean
  onSubmit: SubmitHandler<ISA95ApiBean>
}

const NamespaceForm: FC<NamespaceFormProps> = ({ defaultValues, onSubmit }) => {
  const { t } = useTranslation()
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isValid },
  } = useForm<ISA95ApiBean>({
    defaultValues: defaultValues,
  })
  const isISA95Enabled = watch('enabled')

  return (
    <div>
      <form
        id="namespace-form"
        onSubmit={handleSubmit(onSubmit)}
        style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
      >
        <Flex flexDirection={'column'} mt={8} maxW={600} gap={4}>
          <FormControl>
            <FormLabel htmlFor={'unifiedNamespace-enabled'}>{t('unifiedNamespace.enabled.label')}</FormLabel>
            <Switch id={'unifiedNamespace-enabled'} {...register('enabled')} colorScheme={'brand'} />
            <FormHelperText>{t('unifiedNamespace.enabled.helper')}</FormHelperText>
          </FormControl>

          <FormControl as="fieldset" isDisabled={!isISA95Enabled}>
            <FormLabel as="legend">{t('unifiedNamespace.namespaceGroup.legend')}</FormLabel>
            <FormHelperText>{t('unifiedNamespace.namespaceGroup.helper')}</FormHelperText>

            <Grid templateColumns="repeat(2, 1fr)" gap={6} my={4}>
              <FormControl isDisabled={!isISA95Enabled}>
                <FormLabel htmlFor={'unifiedNamespace-enterprise'}>{t('unifiedNamespace.enterprise.label')}</FormLabel>
                <Input id={'unifiedNamespace-enterprise'} {...register('enterprise')} />
              </FormControl>

              <FormControl isDisabled={!isISA95Enabled}>
                <FormLabel htmlFor={'unifiedNamespace-site'}>{t('unifiedNamespace.site.label')}</FormLabel>
                <Input id={'unifiedNamespace-site'} {...register('site')} />
              </FormControl>

              <FormControl isDisabled={!isISA95Enabled}>
                <FormLabel htmlFor={'unifiedNamespace-area'}>{t('unifiedNamespace.area.label')}</FormLabel>
                <Input id={'unifiedNamespace-area'} {...register('area')} />
              </FormControl>

              <FormControl isDisabled={!isISA95Enabled}>
                <FormLabel htmlFor={'unifiedNamespace-productionLine'}>
                  {t('unifiedNamespace.productionLine.label')}
                </FormLabel>
                <Input id={'unifiedNamespace-productionLine'} {...register('productionLine')} />
              </FormControl>

              <FormControl isDisabled={!isISA95Enabled}>
                <FormLabel htmlFor={'unifiedNamespace-workCell'}>{t('unifiedNamespace.workCell.label')}</FormLabel>
                <Input id={'unifiedNamespace-workCell'} {...register('workCell')} />
              </FormControl>
            </Grid>
          </FormControl>

          <FormControl as="fieldset" isInvalid={!!errors.prefixAllTopics} isDisabled={!isISA95Enabled}>
            <FormLabel as="legend">{t('unifiedNamespace.options.legend')}</FormLabel>
            <Checkbox {...register('prefixAllTopics')}>{t('unifiedNamespace.prefixAllTopics.label')}</Checkbox>
            <FormHelperText> {t('unifiedNamespace.prefixAllTopics.helper')}</FormHelperText>
            <FormErrorMessage>{errors.prefixAllTopics && errors.prefixAllTopics.message}</FormErrorMessage>
          </FormControl>
        </Flex>
      </form>

      <Box mt={16}>
        <Button
          isDisabled={!isValid}
          isLoading={false}
          type="submit"
          colorScheme="yellow"
          variant="solid"
          form="namespace-form"
        >
          Submit
        </Button>
      </Box>
    </div>
  )
}

export default NamespaceForm
