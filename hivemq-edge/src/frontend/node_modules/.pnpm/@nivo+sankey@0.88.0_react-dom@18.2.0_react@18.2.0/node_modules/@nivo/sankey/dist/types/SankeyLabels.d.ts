/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyCommonProps, SankeyNodeDatum } from './types';
interface SankeyLabelsProps<N extends DefaultNode, L extends DefaultLink> {
    nodes: SankeyNodeDatum<N, L>[];
    layout: SankeyCommonProps<N, L>['layout'];
    width: number;
    height: number;
    labelPosition: SankeyCommonProps<N, L>['labelPosition'];
    labelPadding: SankeyCommonProps<N, L>['labelPadding'];
    labelOrientation: SankeyCommonProps<N, L>['labelOrientation'];
    getLabelTextColor: (node: SankeyNodeDatum<N, L>) => string;
}
export declare const SankeyLabels: <N extends DefaultNode, L extends DefaultLink>({ nodes, layout, width, height, labelPosition, labelPadding, labelOrientation, getLabelTextColor, }: SankeyLabelsProps<N, L>) => JSX.Element;
export {};
//# sourceMappingURL=SankeyLabels.d.ts.map