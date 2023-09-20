import React from 'react'

export interface OnboardingAction {
  title: string
  label: string
  to: string
  leftIcon: React.ReactElement
  isExternal?: boolean
}

export interface OnboardingTask {
  header: string
  sections: OnboardingAction[]
  isLoading?: boolean
}
