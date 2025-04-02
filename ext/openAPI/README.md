# Edge OpenAPI Specifications

## Structure 

The main document has been split from the original source in order to reduce the size of a single document and to highlight individual points of concern. 

The original split was done automatically (using `redocly` see below) and the files reflects the automation. 

```
openAPI             
├── README.md
├── openapi.yaml        // root document of the OpenAPI specs
├── paths               // list of path files, organised by their route
│   ├── api_v1_auth_authenticate.yaml
│   ├── api_v1_auth_refresh-token.yaml
│   └── ...
│
└── components          // list of reusable definitions, grouped by type
    ├── headers
    │   ├── ETag.yaml
    │   └── ...
    ├── parameters
    │   ├── CombinerId.yaml
    │   └── ...
    ├── schemas
    │   ├── Adapter.yaml
    │   └── ...
    └── ...
```

There is no reason to maintain such naming convention as we manually add or edit the source.  

## Usage
The [redocly library](https://redocly.com/) is used to manage the OpenAPI specs, stored as a split document. 

Also, the source of truth for all our projects is **ext/hivemq-edge-openapi-<VERSION>-SNAPSHOT.yaml**.

That file will be used for generating all Java and React types.

### Bundle
Some of the tools handling OpenAPi will be working fine with a split document.
If not, consider bundling it into a single `YAML` file, using the following command:

```shell
 redocly bundle <path to source>/openapi.yml -o <path to dest>/hivemq-edge-openapi-SNAPSHOT.yaml
```

Make sure NOT to commit your bundled document along the split origin; we need to ensure a single source of truth. 

### Split 
To reverse the merge operation and create a split document, the following command will deliver: 


```shell
redocly split  <source to file>/openAPI.yaml --outDir <destination folter>/
```

## GitHub guidelines
- The `OpenAPI` specs define in this document is the only source of truth of the `Edge` REST API.
- Any change to the document MUST be done on a new clean PR
- Any change to the document MUST be reviewed and approved by both backend and frontend
- Changes to either backend or frontend code MUST NOT be added to the PR proposing the modifications of the specs
