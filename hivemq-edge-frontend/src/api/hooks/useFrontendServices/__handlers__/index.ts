import type { CapabilityList, PreLoginNotice, GatewayConfiguration, NotificationList } from '@/api/__generated__'
import { Notification, Capability } from '@/api/__generated__'
import { http, HttpResponse } from 'msw'

const lorem =
  'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,'

export const MOCK_PRE_LOGIN_NOTICE: PreLoginNotice = {
  consent: 'I understand and agree to comply',
  title: 'Notice: Entering a Protected System',
  message:
    'I acknowledge that I am bound by the confidentiality obligations imposed through my employment or contractual ' +
    'agreement with MY COMPANY in connection with my access to confidential information, including EDGE and ' +
    'its contents.\n\n' +
    'By entering EDGE, I confirm that I understand my activities within EDGE may be monitored consistent with ' +
    'local law. I also understand that all contents and passwords are confidential information, and that any ' +
    'unauthorized disclosure or use of such confidential information may result in disciplinary action, including ' +
    'termination of my employment or services, and/or legal action based on local law.',
  enabled: true,
}

export const mockGatewayConfiguration: GatewayConfiguration = {
  environment: {
    properties: {
      version: '2023.version',
    },
  },
  cloudLink: {
    displayText: 'HiveMQ Cloud',
    url: 'https://hivemq.com/cloud',
    description: lorem,
    external: true,
  },
  gitHubLink: {
    displayText: 'GitHub',
    url: 'https://github.com/hivemq/hivemq-edge',
    description: lorem,
    external: true,
  },
  documentationLink: {
    displayText: 'Documentation',
    url: 'https://github.com/hivemq/hivemq-edge/wiki',
    description: lorem,
    external: true,
  },
  firstUseInformation: {
    firstUse: false,
    prefillUsername: 'admin',
    prefillPassword: 'password',
    firstUseTitle: 'Welcome To HiveMQ Edge',
    firstUseDescription: lorem,
  },
  ctas: {
    items: [
      {
        displayText: 'Connect My First Device',
        url: './protocol-adapters?from=dashboard-cta',
        description: lorem,
        external: false,
      },
      {
        displayText: 'Connect To My MQTT Broker',
        url: './bridges?from=dashboard-cta',
        description: lorem,
        external: false,
      },
      {
        displayText: 'Learn More',
        url: 'resources?from=dashboard-cta',
        description: lorem,
        external: false,
      },
    ],
  },
  resources: {
    items: [
      {
        displayText: 'Power Of Smart Manufacturing',
        url: 'https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/',
        description: '',
        target: '',
        imageUrl: '',
        external: true,
      },
      {
        displayText: 'Power Of Smart Manufacturing',
        url: 'https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/',
        description: '',
        target: '',
        imageUrl: '',
        external: true,
      },
    ],
  },
  modules: {
    items: [],
  },
  extensions: {
    items: [
      {
        id: 'extension-1',
        version: '1.0.0',
        name: 'My First Extension',
        description: 'Some extension description here which could span multiple lines',
        author: 'HiveMQ',
        priority: 0,
      },
      {
        id: 'hivemq-allow-all-extension',
        version: '1.0.0',
        name: 'Allow All Extension',
        author: 'HiveMQ',
        priority: 0,
        installed: true,
      },
    ],
  },
  hivemqId: 'my-current-installation-id',
  trackingAllowed: true,
  preLoginNotice: MOCK_PRE_LOGIN_NOTICE,
}

export const MOCK_NOTIFICATIONS: Array<Notification> = [
  {
    level: Notification.level.WARNING,
    title: 'Default Credentials Need Changing!',
    description:
      'Your gateway access is configured to use the default username/password combination. This is a security risk. Please ensure you modify your access credentials in your configuration.xml file.',
  },
]

export const MOCK_CAPABILITY_PERSISTENCE: Capability = {
  id: Capability.id.MQTT_PERSISTENCE,
  displayName: 'Persistent Data for MQTT traffic',
  description: 'Mqtt Traffic with QoS greater than 0 is stored persistently on disc and loaded on restart of Edge. ',
}

export const MOCK_CAPABILITY_DATAHUB: Capability = {
  id: Capability.id.DATA_HUB,
  displayName: 'Data Hub for HiveMQ Edge',
  description:
    'This enables HiveMQ Edge to make use of the HiveMQ Data Hub. This includes validation and transformation of data.',
}

export const MOCK_CAPABILITY_WRITEABLE_CONFIG: Capability = {
  id: Capability.id.CONFIG_WRITEABLE,
  displayName: 'Config can be manipulated via the REST API',
  description: 'Changes to the configuration made via the REST API are persisted back into the config.xml.',
}

export const MOCK_CAPABILITY_PULSE_ASSETS: Capability = {
  id: Capability.id.PULSE_ASSET_MANAGEMENT,
  displayName: 'Pulse Asset Management',
  description:
    'This enables HiveMQ Edge to make use of a Pulse Client, including the ability to create, update and map Pulse Assets.',
}

export const MOCK_CAPABILITIES: CapabilityList = {
  items: [MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_DATAHUB, MOCK_CAPABILITY_PULSE_ASSETS],
}

export const handlers = [
  http.get('**/frontend/configuration', () => {
    return HttpResponse.json<GatewayConfiguration>(mockGatewayConfiguration, { status: 200 })
  }),

  http.get('**/frontend/notifications', () => {
    return HttpResponse.json<NotificationList>({ items: MOCK_NOTIFICATIONS }, { status: 200 })
  }),
]

export const handlerCapabilities = (source: CapabilityList) => [
  http.get('**/frontend/capabilities', () => {
    return HttpResponse.json<CapabilityList>(source, { status: 200 })
  }),
]
