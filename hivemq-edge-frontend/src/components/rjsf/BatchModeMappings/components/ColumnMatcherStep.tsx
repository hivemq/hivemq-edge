import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'
import { Select } from 'chakra-react-select'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { Box, HStack, Input, chakra, FormControl, FormErrorMessage } from '@chakra-ui/react'
import { LuChevronsRight } from 'react-icons/lu'

import type { ColumnMappingData, ColumnOption, StepRendererProps } from '@/components/rjsf/BatchModeMappings/types.ts'
import { findMatch } from '@/components/rjsf/BatchModeMappings/utils/levenshtein.utils.ts'

const ColumnMatcherStep: FC<StepRendererProps> = ({ store, onContinue }) => {
  const { schema, worksheet } = store
  const { t } = useTranslation('components')

  const subscriptions = useMemo<ColumnOption[]>(() => {
    const { required, properties } = schema.items as JSONSchema7

    //  TODO[NVL] If not required properties, do we "force" some (assuming a mqtt-topic for example)
    return required
      ? required.map((requiredItem) => {
          const requiredItemProperties = properties?.[requiredItem] as JSONSchema7 | undefined
          return { value: requiredItem, label: requiredItemProperties?.title || requiredItem }
        })
      : []
  }, [schema.items])

  const columns = useMemo<ColumnOption[]>(() => {
    const header = worksheet?.[0]
    //  TODO[NVL] Throw an error and handle failure
    if (!header) return []
    //  TODO[NVL] deal with duplicate headers (seems to be adding _number)
    //  TODO[NVL] deal with no header!
    return Object.keys(header).map((e) => ({ value: e, label: e }))
  }, [worksheet])

  type FormValues = {
    mapping: ColumnMappingData[]
  }

  const {
    register,
    control,
    trigger,
    getValues,
    formState: { errors, isValid },
  } = useForm<FormValues>({
    defaultValues: {
      mapping: subscriptions.map((subscription) => {
        const autoMatch = findMatch(subscription, columns)
        return { column: autoMatch || '', subscription: subscription.value.toString() }
      }),
    },
    mode: 'onChange',
    reValidateMode: 'onChange',
  })
  const { fields } = useFieldArray({
    name: 'mapping',
    control,
  })

  useEffect(() => {
    onContinue({ mapping: undefined })
    trigger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (isValid) {
      const { mapping } = getValues()
      onContinue({ mapping: mapping })
    }
  }, [getValues, isValid, onContinue])

  return (
    <>
      <chakra.form id="batch-mapping-form">
        {fields.map((field, index) => {
          return (
            <FormControl key={field.id} isInvalid={!!errors?.mapping?.[index]?.column} p={4}>
              <HStack>
                <Box flex={1}>
                  <Controller
                    name={`mapping.${index}.column`}
                    control={control}
                    rules={{ required: t('rjsf.batchUpload.columnMapping.validation.required') }}
                    render={({ field: { value, onChange, ...rest } }) => {
                      return (
                        <Select<ColumnOption>
                          {...rest}
                          // TODO[modal-portal] Feels like a hack. Use a ref [?]
                          menuPortalTarget={document.getElementById('chakra-modal-array-field-batch') as HTMLElement}
                          id={`mapping.${index}.column`}
                          instanceId={`mapping.${index}.column`}
                          aria-label={t('rjsf.batchUpload.columnMapping.column.ariaLabel')}
                          onChange={(e) => onChange(e?.label)}
                          value={{ label: value, value }}
                          options={columns}
                        />
                      )
                    }}
                  ></Controller>
                </Box>
                <LuChevronsRight />
                <Box flex={1}>
                  <Input
                    {...register(`mapping.${index}.subscription`, {
                      required: true,
                    })}
                    aria-label={t('rjsf.batchUpload.columnMapping.subscription.ariaLabel')}
                    className={errors?.mapping?.[index]?.subscription ? 'error' : ''}
                    readOnly
                  />
                </Box>
              </HStack>
              <FormErrorMessage id={`mapping.${index}.error`}>
                {errors?.mapping?.[index]?.column?.message}
              </FormErrorMessage>
            </FormControl>
          )
        })}
      </chakra.form>
    </>
  )
}

export default ColumnMatcherStep
