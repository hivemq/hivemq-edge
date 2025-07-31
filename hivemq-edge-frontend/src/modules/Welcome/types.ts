import type React from 'react'

export interface OnboardingAction {
  title: string
  label: string
  to?: string
  isExternal?: boolean
  content?: React.ReactElement
  leftIcon: React.ReactElement
}

export interface OnboardingTask {
  header: string
  sections: OnboardingAction[]
  isLoading?: boolean
}
