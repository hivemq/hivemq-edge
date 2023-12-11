import { Capability, CapabilityList, GatewayConfiguration, Notification, NotificationList } from '@/api/__generated__'
import { rest } from 'msw'

const lorem =
  'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,'

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
  id: 'mqtt-persistence',
  displayName: 'Persistent Data for MQTT traffic',
  description: 'Mqtt Traffic with QoS greater than 0 is stored persistently on disc and loaded on restart of Edge. ',
}

export const MOCK_CAPABILITIES: CapabilityList = { items: [MOCK_CAPABILITY_PERSISTENCE] }

export const handlers = [
  rest.get('**/frontend/configuration', (_, res, ctx) => {
    return res(ctx.json<GatewayConfiguration>(mockGatewayConfiguration), ctx.status(200))
  }),

  rest.get('**/frontend/notifications', (_, res, ctx) => {
    return res(ctx.json<NotificationList>({ items: MOCK_NOTIFICATIONS }), ctx.status(200))
  }),

  rest.get('**/frontend/capabilities', (_, res, ctx) => {
    return res(ctx.json<CapabilityList>(MOCK_CAPABILITIES), ctx.status(200))
  }),
]
