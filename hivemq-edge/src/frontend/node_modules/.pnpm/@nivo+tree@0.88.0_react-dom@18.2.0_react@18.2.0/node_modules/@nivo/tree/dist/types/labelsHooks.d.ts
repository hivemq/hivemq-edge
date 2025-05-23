import { CommonProps, Layout, ComputedNode, ComputedLabel, LabelsPosition } from './types';
export declare const useLabels: <Datum>({ nodes, label, layout, labelsPosition, orientLabel, labelOffset, }: {
    nodes: readonly ComputedNode<Datum>[];
    label: import("@nivo/core").PropertyAccessor<ComputedNode<Datum>, string>;
    layout: Layout;
    labelsPosition: LabelsPosition;
    orientLabel: boolean;
    labelOffset: number;
}) => ComputedLabel<Datum>[];
//# sourceMappingURL=labelsHooks.d.ts.map