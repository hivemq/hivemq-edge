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
  })

export default i18n
