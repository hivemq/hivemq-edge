import { ReactElement } from 'react'

export interface NavLinksBlockType {
  title: string
  items: MainNavLinkType[]
}

export interface MainNavLinkType {
  href: string
  icon: ReactElement
  label?: string
  isActive?: boolean
  isExternal?: boolean
  isDisabled?: boolean
}
