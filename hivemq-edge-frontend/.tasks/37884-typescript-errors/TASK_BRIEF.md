# Task: 37884-typescript-errors

## Objective

Fix the latent TypeScript error in the web app

## Context

Below is a short printout of the errors found in the web app

```console
> npm run build:tsc 2>&1 | tee tsc-full.txt | grep "error TS" | awk -F'(' '{print $1}' | sort | uniq -c | sort -rn

src/components/PaginatedTable/components/Filter.tsx(98,50): error TS2339: Property 'value' does not exist on type '{}'.
src/extensions/datahub/__test-utils__/vitest.utils.ts(4,17): error TS2339: Property 'stringContaining' does not exist on type 'ExpectStatic'.
src/extensions/datahub/components/fsm/ReactFlowRenderer.tsx(131,20): error TS2345: Argument of type 'Node[]' is not assignable to parameter of type 'SetStateAction<never[]>'.
  Type 'Node[]' is not assignable to type 'never[]'.
    Type 'Node' is not assignable to type 'never'.
src/extensions/datahub/components/fsm/ReactFlowRenderer.tsx(132,20): error TS2345: Argument of type 'Edge[]' is not assignable to parameter of type 'SetStateAction<never[]>'.
  Type 'Edge[]' is not assignable to type 'never[]'.
    Type 'Edge' is not assignable to type 'never'.
src/extensions/datahub/components/nodes/BaseNode.tsx(15,15): error TS2322: Type 'unknown' is not assignable to type 'ReactI18NextChildren | Iterable<ReactI18NextChildren>'.
src/extensions/datahub/components/pages/PolicyEditor.tsx(137,17): error TS2322: Type '{ target: string | null; targetHandle: string | null; source: string; sourceHandle: string | null; } | { source: string | null; sourceHandle: string | null; target: string; targetHandle: string | null; }' is not assignable to type 'Connection'.
  Type '{ target: string | null; targetHandle: string | null; source: string; sourceHandle: string | null; }' is not assignable to type 'Connection'.
    Types of property 'target' are incompatible.
      Type 'string | null' is not assignable to type 'string'.
        Type 'null' is not assignable to type 'string'.
src/extensions/datahub/components/pages/PolicyEditor.tsx(191,11): error TS2322: Type '(connection: Connection) => boolean' is not assignable to type 'IsValidConnection<Edge>'.
  Types of parameters 'connection' and 'edge' are incompatible.
    Type 'Edge | Connection' is not assignable to type 'Connection'.
      Type 'Edge' is not assignable to type 'Connection'.
        Types of property 'sourceHandle' are incompatible.
          Type 'string | null | undefined' is not assignable to type 'string | null'.
            Type 'undefined' is not assignable to type 'string | null'.
src/extensions/datahub/components/pages/PolicyEditorLoader.tsx(51,54): error TS2345: Argument of type 'NodeBase' is not assignable to parameter of type 'Node<DataPolicyData>'.
  Type 'NodeBase' is not assignable to type '{ id: string; position: XYPosition; data: DataPolicyData; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
    Types of property 'data' are incompatible.
      Property 'id' is missing in type 'Record<string, unknown>' but required in type 'DataPolicyData'.
src/extensions/datahub/components/pages/PolicyEditorLoader.tsx(52,78): error TS2345: Argument of type 'NodeBase' is not assignable to parameter of type 'Node<DataPolicyData>'.
  Type 'NodeBase' is not assignable to type '{ id: string; position: XYPosition; data: DataPolicyData; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
    Types of property 'data' are incompatible.
      Property 'id' is missing in type 'Record<string, unknown>' but required in type 'DataPolicyData'.
src/extensions/datahub/components/pages/PolicyEditorLoader.tsx(53,103): error TS2345: Argument of type 'NodeBase' is not assignable to parameter of type 'Node<DataPolicyData>'.
  Type 'NodeBase' is not assignable to type '{ id: string; position: XYPosition; data: DataPolicyData; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
    Types of property 'data' are incompatible.
      Property 'id' is missing in type 'Record<string, unknown>' but required in type 'DataPolicyData'.
src/extensions/datahub/components/pages/PolicyEditorLoader.tsx(117,59): error TS2345: Argument of type 'NodeBase' is not assignable to parameter of type 'Node<BehaviorPolicyData>'.
  Type 'NodeBase' is not assignable to type '{ id: string; position: XYPosition; data: BehaviorPolicyData; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
    Types of property 'data' are incompatible.
      Type 'Record<string, unknown>' is missing the following properties from type 'BehaviorPolicyData': id, model
src/extensions/datahub/components/pages/PolicyEditorLoader.tsx(122,9): error TS2345: Argument of type 'NodeBase' is not assignable to parameter of type 'Node<BehaviorPolicyData>'.
  Type 'NodeBase' is not assignable to type '{ id: string; position: XYPosition; data: BehaviorPolicyData; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
    Types of property 'data' are incompatible.
      Type 'Record<string, unknown>' is missing the following properties from type 'BehaviorPolicyData': id, model
src/extensions/datahub/designer/transition/TransitionNode.utils.spec.ts(98,7): error TS2353: Object literal may only specify known properties, and 'positionAbsolute' does not exist in type 'Node<TransitionData>'.
src/extensions/datahub/hooks/useDataHubDraftStore.ts(82,15): error TS2322: Type 'T' is not assignable to type 'Record<string, unknown>'.
src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts(22,36): error TS2345: Argument of type 'T' is not assignable to parameter of type 'DeeplyAllowMatchers<T>'.
src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx(37,26): error TS2322: Type 'UseQueryResult<unknown, Error>[]' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/combiner/components/AutoMapping.spec.cy.tsx(52,24): error TS2322: Type 'UseQueryResult<unknown, Error>[]' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(335,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(414,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(452,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(500,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(530,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(560,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(608,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(646,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(684,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Mappings/hooks/useValidateCombiner.spec.ts(722,9): error TS2345: Argument of type 'UseQueryResult<unknown, Error>[]' is not assignable to parameter of type 'UseQueryResult<DomainTagList | TopicFilterList, Error>[]'.
  Type 'UseQueryResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
    Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'UseQueryResult<DomainTagList | TopicFilterList, Error>'.
      Type 'QueryObserverRefetchErrorResult<unknown, Error>' is not assignable to type 'QueryObserverRefetchErrorResult<DomainTagList | TopicFilterList, Error>'.
        Type 'unknown' is not assignable to type 'DomainTagList | TopicFilterList'.
src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx(51,9): error TS2322: Type 'Node<Group>' is not assignable to type 'NodeGroupType'.
  Type 'Node<Group>' is not assignable to type '{ type: NodeTypes.CLUSTER_NODE; }'.
    Types of property 'type' are incompatible.
      Type 'string | undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
        Type 'undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx(83,9): error TS2322: Type 'Node<Group>' is not assignable to type 'NodeGroupType'.
  Type 'Node<Group>' is not assignable to type '{ type: NodeTypes.CLUSTER_NODE; }'.
    Types of property 'type' are incompatible.
      Type 'string | undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
        Type 'undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx(121,9): error TS2322: Type 'Node<Group>' is not assignable to type 'NodeGroupType'.
  Type 'Node<Group>' is not assignable to type '{ type: NodeTypes.CLUSTER_NODE; }'.
    Types of property 'type' are incompatible.
      Type 'string | undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
        Type 'undefined' is not assignable to type 'NodeTypes.CLUSTER_NODE'.
src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx(31,9): error TS2322: Type 'Node<Bridge | Adapter>' is not assignable to type 'NodeAdapterType | NodeBridgeType'.
  Type 'Node<Bridge | Adapter>' is not assignable to type 'NodeBridgeType'.
    Type 'Node<Bridge | Adapter>' is not assignable to type '{ id: string; position: XYPosition; data: Bridge & { statusModel?: NodeStatusModel | undefined; }; sourcePosition?: Position | undefined; ... 20 more ...; measured?: { ...; } | undefined; }'.
      Types of property 'data' are incompatible.
        Type 'Bridge | Adapter' is not assignable to type 'Bridge & { statusModel?: NodeStatusModel | undefined; }'.
          Type 'Adapter' is not assignable to type 'Bridge & { statusModel?: NodeStatusModel | undefined; }'.
            Type 'Adapter' is missing the following properties from type 'Bridge': cleanStart, host, keepAlive, port, sessionExpiry
src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx(66,9): error TS2322: Type 'Node<Bridge | Adapter>' is not assignable to type 'NodeAdapterType | NodeBridgeType'.
  Type 'Node<Bridge | Adapter>' is not assignable to type 'NodeBridgeType'.
    Type 'Node<Bridge | Adapter>' is not assignable to type '{ id: string; position: XYPosition; data: Bridge & { statusModel?: NodeStatusModel | undefined; }; sourcePosition?: Position | undefined; ... 20 more ...; measured?: { ...; } | undefined; }'.
      Types of property 'data' are incompatible.
        Type 'Bridge | Adapter' is not assignable to type 'Bridge & { statusModel?: NodeStatusModel | undefined; }'.
          Type 'Adapter' is not assignable to type 'Bridge & { statusModel?: NodeStatusModel | undefined; }'.
            Type 'Adapter' is missing the following properties from type 'Bridge': cleanStart, host, keepAlive, port, sessionExpiry
src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx(34,11): error TS2741: Property 'isActive' is missing in type '{ label: string; filter: {}; }' but required in type 'FilterConfigurationOption'.
src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx(35,11): error TS2741: Property 'isActive' is missing in type '{ label: string; filter: {}; }' but required in type 'FilterConfigurationOption'.
src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx(79,65): error TS2741: Property 'isActive' is missing in type '{ label: string; filter: {}; }' but required in type 'FilterConfigurationOption'.
src/modules/Workspace/hooks/useGetPoliciesMatching.ts(40,62): error TS2345: Argument of type 'unknown' is not assignable to parameter of type 'GenericObjectType'.
src/modules/Workspace/hooks/useWorkspaceStore.ts(97,11): error TS2322: Type '(Node | { data: T; id: string; position: XYPosition; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; hidden?: boolean | undefined; ... 25 more ...; domAttributes?: Omit<...> | undefined; })[]' is not assignable to type 'Node[]'.
  Type 'Node | { data: T; id: string; position: XYPosition; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; hidden?: boolean | undefined; ... 25 more ...; domAttributes?: Omit<...> | undefined; }' is not assignable to type 'Node'.
    Type '{ data: T; id: string; position: XYPosition; sourcePosition?: Position; targetPosition?: Position; hidden?: boolean; selected?: boolean; ... 24 more ...; domAttributes?: Omit<HTMLAttributes<HTMLDivElement>, "id" | "style" | "className" | "draggable" | "role" | "aria-label" | "defaultValue" | "dangerouslySetInnerHTML...' is not assignable to type 'Node'.
      Type '{ data: T; id: string; position: XYPosition; sourcePosition?: Position; targetPosition?: Position; hidden?: boolean; selected?: boolean; ... 24 more ...; domAttributes?: Omit<HTMLAttributes<HTMLDivElement>, "id" | "style" | "className" | "draggable" | "role" | "aria-label" | "defaultValue" | "dangerouslySetInnerHTML...' is not assignable to type '{ id: string; position: XYPosition; data: Record<string, unknown>; sourcePosition?: Position | undefined; targetPosition?: Position | undefined; ... 19 more ...; measured?: { ...; } | undefined; }'.
        Types of property 'data' are incompatible.
          Type 'T' is not assignable to type 'Record<string, unknown>'.

```

## Acceptance Criteria

- Keep a concise record of the steps taken to fix specific errors
- Make sure the tests are passing after each fix
- If possible, group the errors in subtask, by complexity of the fix. Keep any complex issue on its own subtask
- Do not undergo any significant refactoring without asking me first

## Extra

```console
npm run build:tsc 2>&1 | tee tsc-full.txt | grep "error TS" | awk -F'(' '{print $1}' | sort | uniq -c | sort -rn > error-summary.txt
```

This will:
Save the complete output to tsc-full.txt
Generate the error count summary in error-summary.txt
