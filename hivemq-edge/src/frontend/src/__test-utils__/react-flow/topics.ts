export const MOCK_TOPIC_ALL = '#'
export const MOCK_TOPIC_BRIDGE_DESTINATION = 'prefix/{#}/bridge/${bridge.name}'
export const MOCK_TOPIC_REF1 = 'root/topic/ref/1'
export const MOCK_TOPIC_REF2 = 'root/topic/ref/2'
export const MOCK_TOPIC_ACT1 = 'root/topic/act/1'

export const MOCK_TOPIC_TREE = {
  name: 'root',
  children: [
    {
      name: 'site1',
      children: [
        {
          name: 'stack',
          // frequency: 56777,
          // count: 30,
          color: 'hsl(185, 70%, 50%)',
          children: [
            {
              name: 'cchart',
              color: 'hsl(34, 70%, 50%)',
              frequency: 24851,
              count: 1,
            },
            {
              name: 'xAxis',
              color: 'hsl(20, 70%, 50%)',
              frequency: 52175,
              count: 1,
            },
            {
              name: 'yAxis',
              color: 'hsl(252, 70%, 50%)',
              frequency: 119087,
              count: 1,
            },
            {
              name: 'layers',
              color: 'hsl(160, 70%, 50%)',
              frequency: 118943,
              count: 1,
            },
          ],
        },
        {
          name: 'ppie',
          color: 'hsl(3, 70%, 50%)',
          children: [
            {
              name: 'chart',
              color: 'hsl(336, 70%, 50%)',
              children: [
                {
                  name: 'pie',
                  color: 'hsl(211, 70%, 50%)',
                  children: [
                    {
                      name: 'outline',
                      color: 'hsl(307, 70%, 50%)',
                      frequency: 119582,
                      count: 1,
                    },
                    {
                      name: 'slices',
                      color: 'hsl(348, 70%, 50%)',
                      frequency: 85050,
                      count: 1,
                    },
                    {
                      name: 'bbox',
                      color: 'hsl(182, 70%, 50%)',
                      frequency: 186459,
                      count: 1,
                    },
                  ],
                },
                {
                  name: 'donut',
                  color: 'hsl(27, 70%, 50%)',
                  frequency: 115199,
                  count: 1,
                },
                {
                  name: 'gauge',
                  color: 'hsl(335, 70%, 50%)',
                  frequency: 144065,
                  count: 1,
                },
              ],
            },
            {
              name: 'legends',
              color: 'hsl(318, 70%, 50%)',
              frequency: 135671,
              count: 1,
            },
          ],
        },
      ],
    },
    {
      name: 'site2',
      children: [
        {
          name: 'rgb',
          color: 'hsl(59, 70%, 50%)',
          frequency: 33593,
          count: 1,
        },
        {
          name: 'hsl',
          color: 'hsl(52, 70%, 50%)',
          frequency: 171582,
          count: 1,
        },
      ],
    },
    {
      name: 'site3',
      children: [
        {
          name: 'randomize',
          color: 'hsl(224, 70%, 50%)',
          frequency: 199708,
          count: 1,
        },
        {
          name: 'resetClock',
          color: 'hsl(254, 70%, 50%)',
          frequency: 18326,
          count: 1,
        },
        {
          name: 'noop',
          color: 'hsl(125, 70%, 50%)',
          frequency: 194242,
          count: 1,
        },
        {
          name: 'tick',
          color: 'hsl(171, 70%, 50%)',
          frequency: 102804,
          count: 1,
        },
        {
          name: 'forceGC',
          color: 'hsl(283, 70%, 50%)',
          frequency: 155600,
          count: 1,
        },
        {
          name: 'stackTrace',
          color: 'hsl(199, 70%, 50%)',
          frequency: 46202,
          count: 1,
        },
        {
          name: 'dbg',
          color: 'hsl(77, 70%, 50%)',
          frequency: 161397,
          count: 1,
        },
      ],
    },
  ],
}
