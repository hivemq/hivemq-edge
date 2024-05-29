import { FC, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Select } from 'chakra-react-select'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { Box, HStack, Input, chakra, FormControl, FormErrorMessage } from '@chakra-ui/react'
import { LuChevronsRight } from 'react-icons/lu'

import { ColumnMappingData, ColumnOption, StepRendererProps } from '@/components/rjsf/BatchSubscription/types.ts'
import { findMatch } from '@/components/rjsf/BatchSubscription/utils/levenshtein.utils.ts'

const ColumnMatcherStep: FC<StepRendererProps> = ({ store }) => {
  const { schema, worksheet } = store
  const { t } = useTranslation('components')

  const subscriptions = useMemo<ColumnOption[]>(() => {
    const { required, properties } = schema.items as JSONSchema7

    //  TODO[NVL] If not required properties, do we "force" some (assuming a mqtt-topic for example)
    return required
      ? required.map((e) => {
          const sss = properties?.[e] as JSONSchema7 | undefined
          return { value: e, label: sss?.title || e }
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
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      mapping: subscriptions.map((e) => {
        const autoMatch = findMatch(e, columns)

        return { column: autoMatch || '', subscription: e.label }
      }),
    },
    mode: 'onChange',
    reValidateMode: 'onChange',
  })
  const { fields } = useFieldArray({
    name: 'mapping',
    control,
  })

  const onSubmit = (data: FormValues) => console.log(data)

  useEffect(() => {
    trigger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <>
      <chakra.form onSubmit={handleSubmit(onSubmit)} id="SSSSSS">
        {fields.map((field, index) => {
          return (
            <FormControl key={field.id} isInvalid={!!errors?.mapping?.[index]?.column} p={4}>
              <HStack>
                <Box flex={1}>
                  <Controller
                    name={`mapping.${index}.column`}
                    control={control}
                    rules={{ required: t('rjsf.batchUpload.columnMapping.validation.required', { prop: '' }) }}
                    render={({ field: { value, onChange, ...rest } }) => {
                      return (
                        <Select<ColumnOption>
                          {...rest}
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
                    readOnly
                    placeholder="quantity"
                    {...register(`mapping.${index}.subscription`, {
                      required: true,
                    })}
                    className={errors?.mapping?.[index]?.subscription ? 'error' : ''}
                  />
                </Box>
              </HStack>
              <FormErrorMessage>{errors?.mapping?.[index]?.column?.message}</FormErrorMessage>
            </FormControl>
          )
        })}
      </chakra.form>
      <input type="submit" form="SSSSSS" />
    </>
  )
}

export default ColumnMatcherStep
