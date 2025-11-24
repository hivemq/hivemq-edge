/* istanbul ignore file -- @preserve */
import { initReactI18next } from 'react-i18next'
import i18n from 'i18next'
import Pseudo from 'i18next-pseudo'

import main_en from '@/locales/en/translation.json'
import component_en from '@/locales/en/components.json'
import schema_en from '@/locales/en/schemas.json'
import datahub_en from '@/extensions/datahub/locales/en/datahub.json'

const resources = {
  en: {
    translation: { ...main_en },
    components: { ...component_en },
    schemas: { ...schema_en },
    datahub: { ...datahub_en },
  },
}

i18n
  .use(
    new Pseudo({
      enabled: false,
      languageToPseudo: 'en',
      letterMultiplier: 4,
      repeatedLetters: ['B', 'o', 'a', 't'],
    })
  )
  .use(initReactI18next)
  .init({
    resources,
    ns: ['translation', 'components', 'schemas'],
    defaultNS: 'translation',
    lng: 'en',
    debug: import.meta.env.MODE === 'development',
    interpolation: {
      escapeValue: false, // react already safes from xss
    },
    postProcess: ['pseudo'],
    // Only detect missing keys in development mode
    // saveMissing controls when missingKeyHandler is called
    saveMissing: import.meta.env.MODE === 'development',
    missingKeyHandler: (lngs: readonly string[], ns: string, key: string) => {
      if (typeof window !== 'undefined') {
        // Store missing keys for Cypress tests to check
        // @ts-expect-error - Adding custom property for test detection
        window.__i18nextMissingKeys = window.__i18nextMissingKeys || []
        // @ts-expect-error - Adding custom property for test detection
        window.__i18nextMissingKeys.push({ key, ns, lngs })
      }
    },
  })

export default i18n
