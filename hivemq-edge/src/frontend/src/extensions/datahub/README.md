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
