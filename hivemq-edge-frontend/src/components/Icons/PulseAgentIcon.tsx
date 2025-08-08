import type { FC } from 'react'
import { createIcon, Icon, type IconProps } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

const PulseAgentSVG = createIcon({
  displayName: 'PulseAgentIcon',
  viewBox: '0 0 64 64',
  path: (
    <>
      <path d="M13.9099 49.1599L49.1699 49.1599L49.1699 13.8999L13.9099 13.8999L13.9099 49.1599Z" fill="white" />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M49.6699 49.6599L13.4099 49.6599L13.4099 13.3999L49.6699 13.3999L49.6699 49.6599ZM48.6699 48.6599L48.6699 14.3999L14.4099 14.3999L14.4099 48.6599L48.6699 48.6599Z"
        fill="black"
      />
      <path fillRule="evenodd" clipRule="evenodd" d="M46.3799 52V57.42H45.3799V52H46.3799Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M39.2202 52V57.42H38.2202V52H39.2202Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M32.0601 52V57.42H31.0601V52H32.0601Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M24.8901 52V57.42H23.8901V52H24.8901Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M17.73 52V57.42H16.73V52H17.73Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M11.43 46.1899H6V45.1899H11.43V46.1899Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M11.43 39.03H6V38.03H11.43V39.03Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M11.43 31.8601H6V30.8601H11.43V31.8601Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M11.43 24.7H6V23.7H11.43V24.7Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M11.43 17.54H6V16.54H11.43V17.54Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M52 16.6799H57.43V17.6799H52V16.6799Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M52 23.8501H57.43V24.8501H52V23.8501Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M52 31.01H57.43V32.01H52V31.01Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M52 38.1699H57.43V39.1699H52V38.1699Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M52 45.3301H57.43V46.3301H52V45.3301Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M16.5498 11.43V6H17.5498V11.43H16.5498Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M23.71 11.43V6H24.71V11.43H23.71Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M30.8799 11.43V6H31.8799V11.43H30.8799Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M38.04 11.43V6H39.04V11.43H38.04Z" fill="black" />
      <path fillRule="evenodd" clipRule="evenodd" d="M45.2002 11.43V6H46.2002V11.43H45.2002Z" fill="black" />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M27.6223 39.3584C27.6225 39.3582 27.6227 39.3581 27.3202 38.96C27.0176 38.5619 27.0179 38.5617 27.0181 38.5616L27.0186 38.5612L27.02 38.5601L27.0238 38.5573L27.0359 38.5483C27.0459 38.541 27.0598 38.5309 27.0775 38.5183C27.113 38.4932 27.1636 38.4582 27.2287 38.4157C27.3586 38.3306 27.5464 38.2151 27.7848 38.0875C28.2607 37.8327 28.9439 37.5272 29.7761 37.3231C31.4505 36.9125 33.724 36.9166 36.0845 38.5487L35.5158 39.3713C33.4363 37.9334 31.4698 37.9375 30.0142 38.2944C29.2814 38.4741 28.6771 38.7441 28.2567 38.9691C28.047 39.0814 27.8845 39.1816 27.7762 39.2525C27.7221 39.2879 27.6816 39.3159 27.6557 39.3342C27.6427 39.3434 27.6334 39.3502 27.6279 39.3542L27.6224 39.3583C27.622 39.3586 27.6218 39.3588 27.6218 39.3587L27.6221 39.3586L27.6223 39.3584Z"
        fill="black"
      />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M24.0002 33.6571C24.0003 33.657 24.0005 33.6569 23.71 33.2499C23.4195 32.843 23.4197 32.8428 23.4199 32.8426L23.4224 32.8409L23.4281 32.8369L23.4477 32.8233C23.4643 32.8118 23.4881 32.7955 23.5189 32.775C23.5805 32.7341 23.67 32.676 23.7857 32.6049C24.0171 32.4627 24.3539 32.2679 24.7829 32.0522C25.6402 31.6213 26.8706 31.1048 28.3678 30.7602C31.3697 30.0692 35.4448 30.0715 39.7019 32.8303L39.1581 33.6695C35.1751 31.0883 31.3902 31.0906 28.5921 31.7347C27.1893 32.0576 26.0347 32.5423 25.232 32.9457C24.8311 33.1473 24.5191 33.3279 24.3093 33.4569C24.2044 33.5214 24.1251 33.5728 24.073 33.6075C24.047 33.6248 24.0278 33.6379 24.0157 33.6463L24.0027 33.6553L24.0002 33.6571Z"
        fill="black"
      />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M19.0876 27.8885C19.0877 27.8884 19.0879 27.8884 18.8301 27.46C18.5722 27.0315 18.5725 27.0314 18.5728 27.0312L18.5736 27.0307L18.576 27.0293L18.5842 27.0244L18.6138 27.0071C18.6394 26.9922 18.6765 26.9709 18.7249 26.9437C18.8216 26.8894 18.9633 26.8119 19.1473 26.7166C19.5154 26.5259 20.053 26.2637 20.7389 25.973C22.11 25.392 24.0771 24.6958 26.4685 24.2316C31.2538 23.3028 37.7485 23.3021 44.55 27.0213L44.0702 27.8986C37.5216 24.3178 31.2763 24.3171 26.6591 25.2133C24.3492 25.6616 22.4495 26.3342 21.1291 26.8938C20.4691 27.1734 19.9548 27.4245 19.6074 27.6045C19.4337 27.6945 19.3017 27.7667 19.2142 27.8158C19.1705 27.8403 19.1379 27.8591 19.1167 27.8714L19.0936 27.885L19.0885 27.888L19.0877 27.8885L19.0876 27.8885Z"
        fill="black"
      />
    </>
  ),
})

export const PulseAgentIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')
  return <Icon as={PulseAgentSVG} boxSize={4} aria-label={t('iconLabel.pulse')} {...props} />
}
