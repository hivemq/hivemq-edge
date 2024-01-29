## DataHub frontend extension

This bundle is organising the `DataHub` features as a self-contained "extension", preparing the code for its
extraction in a proper reusable structure, ready for usage with the rest of the `HiveMQ` ecosystem.

We are not yet implementing a fully-fledged extension system for the frontend but the `Datahub` features have been
organised in a way that should make the move easier

All the code relevant to the extension will therefore be in this directory.
Dependencies with the rest of the code (`Edge` in this case) is limited to as few imports as possible

## APIs

- OpenAPI specs for `DataHub` are merged with `Edge` and the stubs are therefore all incorporated in the
  `src/api/__generated__` directory
- The `React Query` hooks however are in the extension (`src/extensions/datahub/api/hooks`) and rely on the shared code
  (in particular the `useHttpClient`)

## Schemas

TODO

- JSONSchemas used for the UI forms are in `src/extensions/datahub/api/__generated__/schemas`.
  They are not generated but created by hand from the API and the documentation. #
  The following schemas have been added (using the name of their matching models):
  - `BehaviorPolicyData.jon`
  - `ClientFilterData.jon`
  - `OperationData.json`

## MSW

If running the frontend with `Mock Service Worker`, the `datahub` handlers need to be added to the main worker:

```typescript jsx
// src/__test-utils__/msw/handlers.ts
import { handlers as DataHubDataPoliciesService } from '@/extensions/datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { handlers as DataHubBehaviorPoliciesService } from '@/extensions/datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { handlers as DataHubSchemasService } from '@/extensions/datahub/api/hooks/DataHubSchemasService/__handlers__'
import { handlers as DataHubScriptsService } from '@/extensions/datahub/api/hooks/DataHubScriptsService/__handlers__'

export const handlers = [
  // other handers
  ...DataHubDataPoliciesService,
  ...DataHubBehaviorPoliciesService,
  ...DataHubSchemasService,
  ...DataHubScriptsService,
]
```

## Internationalisation

`DataHub` contains its own translation, located in `./locales/en/datahub.json`.
They need to be integrated with the destination's `i18next` configuration:

```typescript
// src/config/i18n.config.ts
import datahub_en from '@/extensions/datahub/locales/en/datahub.json'

const resources = {
  en: {
    // other resources
    datahub: { ...datahub_en },
  },
}

i18n.use(initReactI18next).init({
  resources,
  // rest of the configuyration
  lng: 'en',
  defaultNS: 'translation',
  // [...]
})
```

Note that the translation hook is constantly using the `datahub` namespace so make sure to use it for
the name of the resource. It doesn't have to be added to the list of namespaces.

## In-app navigation and routing

- todo

## Third-party dependencies

The following libraries are used for `DataHub` and should be installed in the destination app.

- react-router-dom
- @chakra-ui/react
- react-icons/
- react-i18next
- @tanstack/react-query
- tanstack/react-table
- reactflow

## UI dependencies

- ChakraUI is used for the UI framework and widgets. The theme used for `Edge` is also, by embedding, used for `DataHub`.
  The theme can be found in `src/modules/Theme`. There has been no change specifically made for `DataHub`.
- There are a few UI components that have been deemed "reusable", i.e. worth exporting to a separate repo at some point.
  They are located in the `src/components` directory of `Edge`. The following components are used by `DataHub` and are
  expected to be migrated as well:

  - `PageContainer`
  - `DateTimeRenderer`
  - `PaginatedTable`
  - `IconButton`
