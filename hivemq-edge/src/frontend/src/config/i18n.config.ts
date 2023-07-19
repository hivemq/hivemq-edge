import { initReactI18next } from 'react-i18next'
import i18n from 'i18next'
import Pseudo from 'i18next-pseudo'

import en from '../locales/en/translation.json'

const resources = {
  en: {
    translation: { ...en },
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
    lng: 'en',
    debug: import.meta.env.MODE === 'development',
    interpolation: {
      escapeValue: false, // react already safes from xss
    },
    postProcess: ['pseudo'],
  })

export default i18n
