## Adapters

The adapters are solely defined by their JSONSchema and don't necessarily abide to a common structure (for the time being).
But when we need to programmatically exploit parts of their data (e.g. extract `host` or `topcis`), we don't have a common
routine to do so.

The solution adopted for the moment is to generate a Typescript document based on the JSONSchema for each adapter added to
the system.

We are using `json-schema-to-typescript` (https://github.com/bcherny/json-schema-to-typescript)

The files are all in this directory.
