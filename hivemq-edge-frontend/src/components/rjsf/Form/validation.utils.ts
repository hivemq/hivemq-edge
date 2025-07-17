import { customizeValidator } from '@rjsf/validator-ajv8'

// No exposed from '@rjsf/validator-ajv8'
export interface ErrorObject<K extends string = string, P = Record<string, unknown>, S = unknown> {
  keyword: K
  instancePath: string
  schemaPath: string
  params: P
  propertyName?: string
  message?: string
  schema?: S
  parentSchema?: unknown
  data?: unknown
}

import i18n from '@/config/i18n.config.ts'

// TODO[NVL] Initially crafted as /^[^+#$]*$/; is $ allowed?
export const validationTopic = (data: string): string | undefined => {
  if (data.length === 0) return i18n.t('rjsf.customFormats.validation.noEmptyString', { ns: 'components' })
  if (data.includes('\u0000')) return i18n.t('rjsf.customFormats.validation.noNullChar', { ns: 'components' })
  if (data.includes('#') || data.includes('+'))
    return i18n.t('rjsf.customFormats.validation.noWildCards', { ns: 'components' })
  return undefined
}

export const validationTopicFilter = (topic: string): string | undefined => {
  if (topic.length === 0) return i18n.t('rjsf.customFormats.validation.noEmptyString', { ns: 'components' })
  if (topic.includes('\u0000')) return i18n.t('rjsf.customFormats.validation.noNullChar', { ns: 'components' })

  const SHARED_SUBSCRIPTION_CHAR_ARRAY = '$share'.split('')
  const SHARED_SUBSCRIPTION_LENGTH = SHARED_SUBSCRIPTION_CHAR_ARRAY.length
  const SHARED_SUBSCRIPTION_DELIMITER = '/'

  let lastChar = topic.charAt(0)
  let currentChar
  let sharedSubscriptionDelimiterCharCount = 0
  const length = topic.length
  let isSharedSubscription = false
  let sharedCounter = lastChar == SHARED_SUBSCRIPTION_CHAR_ARRAY[0] ? 1 : -1

  for (let i = 1; i < length; i++) {
    currentChar = topic.charAt(i)

    // current char still matching $share ?
    if (i < SHARED_SUBSCRIPTION_LENGTH && currentChar == SHARED_SUBSCRIPTION_CHAR_ARRAY[i]) {
      sharedCounter++
    }

    // finally, is it a shared subscription?
    if (
      i == SHARED_SUBSCRIPTION_LENGTH &&
      sharedCounter == SHARED_SUBSCRIPTION_LENGTH &&
      currentChar == SHARED_SUBSCRIPTION_DELIMITER
    ) {
      isSharedSubscription = true
    }

    //Check the shared name
    if (isSharedSubscription && sharedSubscriptionDelimiterCharCount == 1) {
      if (currentChar == '+' || currentChar == '#') {
        //Shared name contains wildcard chars
        return i18n.t('rjsf.customFormats.validation.noWildCardsSharedName', { ns: 'components' })
      }
      if (lastChar == SHARED_SUBSCRIPTION_DELIMITER && currentChar == SHARED_SUBSCRIPTION_DELIMITER) {
        //Check if the shared name is empty
        return i18n.t('rjsf.customFormats.validation.noEmptySharedName', { ns: 'components' })
      }
    }

    // how many times did we see the sharedSubscriptionDelimiter?
    if (isSharedSubscription && currentChar == SHARED_SUBSCRIPTION_DELIMITER) {
      sharedSubscriptionDelimiterCharCount++
    }

    // If the last character is a # and is prepended with /, then it's a valid subscription
    if (i == length - 1 && currentChar == '#' && lastChar == '/') {
      return undefined
    }

    //Check if something follows after the # sign
    if (lastChar == '#' || (currentChar == '#' && i == length - 1)) {
      return i18n.t('rjsf.customFormats.validation.noMultiLevelString', { ns: 'components' })
    }

    //Let's check if the + sign is in the middle of a string
    if (currentChar == '+' && lastChar != '/') {
      if (
        sharedSubscriptionDelimiterCharCount != 2 ||
        !isSharedSubscription ||
        lastChar != SHARED_SUBSCRIPTION_DELIMITER
      ) {
        return i18n.t('rjsf.customFormats.validation.noSingleLevelString', { ns: 'components' })
      }
    }
    //Let's check if the + sign is followed by a
    if (lastChar == '+' && currentChar != '/') {
      return i18n.t('rjsf.customFormats.validation.noSingleLevelFinal', { ns: 'components' })
    }
    lastChar = currentChar
  }

  return undefined
}

// TODO[NVL] Currently like topic but what about the + and # chars?
export const validationTag = validationTopic

export const customLocalizer = (errors?: null | ErrorObject[]) => {
  if (!errors) return

  for (const error of errors) {
    if (error.keyword !== 'format') continue
    if (error.schema === 'mqtt-topic') {
      error.message = validationTopic(error.data as string)
      continue
    }
    if (error.schema === 'mqtt-topic-filter') {
      error.message = validationTopicFilter(error.data as string)
    }
    if (error.schema === 'mqtt-tag') {
      error.message = validationTag(error.data as string)
    }
  }
}

export const customFormatsValidator = customizeValidator(
  {
    customFormats: {
      // TODO[26559] This is a hack to remove the error; fix at source
      ['boolean']: () => true,
      // TODO[33325] This is a hack to remove the error; fix at source
      interpolation: () => true,
      identifier: () => true,
      'mqtt-topic': (topic) => validationTopic(topic) === undefined,
      'mqtt-tag': (tag) => validationTag(tag) === undefined,
      'mqtt-topic-filter': (topicFilter) => validationTopicFilter(topicFilter) === undefined,
    },
  },
  customLocalizer
)
