// Config schema for static directories
export const configSchema = {
  static: {
    type: 'object',
    $ref: '/snapshot#/$defs/filter',
    unevaluatedProperties: false,
    properties: {
      baseUrl: {
        $ref: '/snapshot/server#/properties/baseUrl'
      },
      cleanUrls: {
        $ref: '/snapshot/server#/properties/cleanUrls'
      },
      rewrites: {
        $ref: '/snapshot/server#/properties/rewrites'
      },
      options: {
        $ref: '/snapshot#/$defs/options'
      }
    }
  },
  sitemap: {
    type: 'object',
    $ref: '/snapshot#/$defs/filter',
    unevaluatedProperties: false,
    properties: {
      options: {
        $ref: '/snapshot#/$defs/options'
      }
    }
  }
};
export function configMigration(config, util) {
  /* eslint-disable curly */
  if (config.version < 2) {
    // static-snapshots and options were renamed
    util.map('staticSnapshots.baseUrl', 'static.baseUrl');
    util.map('staticSnapshots.snapshotFiles', 'static.include');
    util.map('staticSnapshots.ignoreFiles', 'static.exclude');
    util.del('staticSnapshots');
  }
}