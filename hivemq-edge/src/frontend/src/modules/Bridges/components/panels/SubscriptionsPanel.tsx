import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Controller, useFieldArray } from 'react-hook-form'
import { CreatableSelect } from 'chakra-react-select'
import {
  Accordion,
  AccordionItem,
  AccordionButton,
  AccordionPanel,
  AccordionIcon,
  Box,
  ButtonGroup,
  FormControl,
  FormErrorMessage,
  FormLabel,
  IconButton,
  VStack,
  Flex,
  Card,
  CardBody,
  FormHelperText,
  Stack,
  HStack,
  CardHeader,
  RadioGroup,
  Radio,
  Switch,
} from '@chakra-ui/react'
import { AddIcon, DeleteIcon } from '@chakra-ui/icons'

import { $BridgeSubscription } from '@/api/__generated__'
import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'
import CustomUserProperties from './CustomUserProperties.tsx'
import { BridgeSubscriptionsProps } from '../../types.ts'

const SubscriptionsPanel: FC<BridgeSubscriptionsProps> = ({ form, type }) => {
  const { t } = useTranslation()
  const { fields, prepend, remove } = useFieldArray({
    control: form.control, // control props comes from useForm (optional: if you are using FormContext)
    name: type, // unique name for your Field Array
  })
  const getRulesForProperty = useValidationRules()

  const {
    register,
    formState: { errors },
  } = form

  return (
    <>
      <VStack spacing={4} align="stretch" mt={4}>
        {fields.map((field, index) => {
          return (
            <Card shadow="md" borderWidth="1px" flexDirection={'column'} key={field.id}>
              <HStack>
                <CardBody>
                  <Flex gap={4}>
                    <FormControl
                      data-testid={`${type}.${index}.filters`}
                      isInvalid={!!errors[type]?.[index]?.filters}
                      isRequired
                    >
                      <FormLabel htmlFor={`${type}.${index}.filters`}>
                        {t('bridge.subscription.filters.label')}
                      </FormLabel>
                      <Controller
                        name={`${type}.${index}.filters`}
                        render={({ field }) => {
                          const { value, onChange, ...rest } = field
                          const formatValue = value.map((e) => ({ value: e, label: e }))
                          return (
                            <CreatableSelect
                              {...rest}
                              value={formatValue}
                              onChange={(values) => onChange(values.map((item) => item.value))}
                              inputId={`${type}.${index}.filters`}
                              isClearable={true}
                              isMulti={true}
                              components={{
                                DropdownIndicator: null,
                              }}
                            />
                          )
                        }}
                        control={form.control}
                        rules={{
                          ...getRulesForProperty($BridgeSubscription.properties.filters),
                        }}
                      />
                      {!errors[type]?.[index]?.filters && (
                        <FormHelperText>{t('bridge.subscription.filters.helper')}</FormHelperText>
                      )}
                      <FormErrorMessage>{errors[type]?.[index]?.filters?.message}</FormErrorMessage>
                    </FormControl>

                    <FormControl
                      data-testid={`${type}.${index}.destination`}
                      isInvalid={!!errors[type]?.[index]?.destination}
                      isRequired
                    >
                      <FormLabel htmlFor={`${type}.${index}.destination`}>
                        {t('bridge.subscription.destination.label')}
                      </FormLabel>
                      <Controller
                        name={`${type}.${index}.destination`}
                        render={({ field }) => {
                          const { value, onChange, ...rest } = field
                          const formatValue = { value: value, label: value }
                          return (
                            <CreatableSelect
                              inputId={`${type}.${index}.destination`}
                              {...rest}
                              value={formatValue}
                              onChange={(item) => onChange(item?.value)}
                              options={[{ value: '{#}', label: '{#} - original message topic' }]}
                              isClearable={true}
                              isMulti={false}
                              components={{
                                DropdownIndicator: null,
                              }}
                            />
                          )
                        }}
                        control={form.control}
                        rules={{
                          ...getRulesForProperty($BridgeSubscription.properties.destination),
                        }}
                      />
                      {!errors[type]?.[index]?.destination && (
                        <FormHelperText>{t('bridge.subscription.destination.helper')}</FormHelperText>
                      )}
                      <FormErrorMessage>{errors[type]?.[index]?.destination?.message}</FormErrorMessage>
                    </FormControl>
                  </Flex>
                </CardBody>
                <CardHeader alignSelf={'flex-start'}>
                  <ButtonGroup size="sm" isAttached variant="outline">
                    <IconButton
                      onClick={() => remove(index)}
                      aria-label={t('bridge.subscription.delete')}
                      icon={<DeleteIcon />}
                    />
                  </ButtonGroup>
                </CardHeader>
              </HStack>
              <CardBody p={0}>
                <Accordion allowMultiple>
                  <AccordionItem isDisabled={!!errors[type]?.[index]} data-testid={`${type}.${index}.advanced`}>
                    <AccordionButton>
                      <AccordionIcon />
                      <Box as="span" flex="1" textAlign="left">
                        Advanced configuration
                        <FormErrorMessage>{errors[type]?.[index]?.filters?.message}</FormErrorMessage>
                      </Box>
                    </AccordionButton>

                    <AccordionPanel m={1}>
                      <FormControl>
                        <FormLabel htmlFor={`${type}.${index}.maxQoS`}>
                          {t('bridge.subscription.maxQoS.label')}
                        </FormLabel>
                        <Controller
                          name={`${type}.${index}.maxQoS`}
                          control={form.control}
                          render={({ field: { value, ...rest } }) => (
                            <RadioGroup {...rest} value={value.toString()} id={`${type}.${index}.maxQoS`}>
                              <Stack direction="row">
                                <Radio value="0">{t('bridge.subscription.maxQoS.values.0')}</Radio>
                                <Radio value="1">{t('bridge.subscription.maxQoS.values.1')}</Radio>
                                <Radio value="2">{t('bridge.subscription.maxQoS.values.2')}</Radio>
                              </Stack>
                            </RadioGroup>
                          )}
                          rules={{
                            ...getRulesForProperty($BridgeSubscription.properties.maxQoS),
                          }}
                        />
                      </FormControl>

                      <FormControl>
                        <FormLabel htmlFor={`${type}.${index}.excludes`}>{t('bridge.subscription.excludes')}</FormLabel>
                        <Controller
                          name={`${type}.${index}.excludes`}
                          render={({ field }) => {
                            const { value, onChange, ...rest } = field
                            const formatValue = value?.map((e) => ({ value: e, label: e }))
                            return (
                              <CreatableSelect
                                {...rest}
                                value={formatValue}
                                onChange={(values) => onChange(values.map((item) => item.value))}
                                inputId={`${type}.${index}.excludes`}
                                // options={[{ value: 'ddfd', label: 'fgg' }]}
                                // menuIsOpen={false}
                                isClearable={true}
                                placeholder={'add topic'}
                                isMulti={true}
                                components={{
                                  DropdownIndicator: null,
                                }}
                              />
                            )
                          }}
                          control={form.control}
                        />
                        <FormErrorMessage>{errors[type]?.[index]?.filters?.message}</FormErrorMessage>
                      </FormControl>

                      <FormControl>
                        <FormLabel htmlFor={`${type}.${index}.preserveRetain`}>
                          {t('bridge.subscription.preserveRetain.label')}
                        </FormLabel>
                        <Switch
                          id={`${type}.${index}.preserveRetain`}
                          {...register(`${type}.${index}.preserveRetain`)}
                        />
                        <FormErrorMessage>{errors[type]?.[index]?.filters?.message}</FormErrorMessage>
                      </FormControl>

                      <CustomUserProperties type={type} subscriptionIndex={index} form={form} />
                    </AccordionPanel>
                  </AccordionItem>
                </Accordion>
              </CardBody>
            </Card>
          )
        })}
        <Box>
          <IconButton
            data-testid={'bridge-subscription-add'}
            isDisabled={!!errors[type]}
            aria-label={t('bridge.subscription.add')}
            icon={<AddIcon />}
            onClick={() => prepend({ destination: '', filters: [], maxQoS: 0 })}
          />
        </Box>
      </VStack>
    </>
  )
}

export default SubscriptionsPanel
