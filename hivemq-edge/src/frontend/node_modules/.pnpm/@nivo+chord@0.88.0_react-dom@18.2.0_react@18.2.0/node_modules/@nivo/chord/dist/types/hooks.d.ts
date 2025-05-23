/// <reference types="react" />
import { OrdinalColorScale } from '@nivo/colors';
import { ArcDatum, ChordCommonProps, ChordDataProps, CustomLayerProps, RibbonDatum } from './types';
import { ChordLayout } from 'd3-chord';
export declare const useChordLayout: ({ padAngle }: {
    padAngle: ChordCommonProps['padAngle'];
}) => ChordLayout;
export declare const useChordGenerators: ({ width, height, innerRadiusRatio, innerRadiusOffset, }: {
    width: number;
    height: number;
    innerRadiusRatio: ChordCommonProps['innerRadiusRatio'];
    innerRadiusOffset: ChordCommonProps['innerRadiusOffset'];
}) => {
    center: [number, number];
    radius: number;
    innerRadius: number;
    arcGenerator: import("./types").ArcGenerator;
    ribbonGenerator: import("./types").RibbonGenerator;
};
export declare const useChordArcsAndRibbons: ({ chord, getColor, keys, data, getLabel, formatValue, }: {
    chord: ChordLayout;
    data: ChordDataProps['data'];
    keys: ChordDataProps['keys'];
    getLabel: (arc: Omit<ArcDatum, 'label' | 'color'>) => string;
    formatValue: (value: number) => string;
    getColor: OrdinalColorScale<Omit<ArcDatum, 'label' | 'color'>>;
}) => {
    arcs: ArcDatum[];
    ribbons: RibbonDatum[];
};
export declare const useChord: ({ data, keys, label, valueFormat, width, height, innerRadiusRatio, innerRadiusOffset, padAngle, colors, }: {
    data: ChordDataProps['data'];
    keys: ChordDataProps['keys'];
    label?: import("@nivo/core").PropertyAccessor<Omit<ArcDatum, "label" | "color">, string> | undefined;
    valueFormat?: import("@nivo/core").ValueFormat<number, void> | undefined;
    width: number;
    height: number;
    innerRadiusRatio?: number | undefined;
    innerRadiusOffset?: number | undefined;
    padAngle?: number | undefined;
    colors?: import("@nivo/colors").OrdinalColorScaleConfig<Omit<ArcDatum, "label" | "color">> | undefined;
}) => {
    center: [number, number];
    chord: ChordLayout;
    radius: number;
    innerRadius: number;
    arcGenerator: import("./types").ArcGenerator;
    ribbonGenerator: import("./types").RibbonGenerator;
    getColor: OrdinalColorScale<Omit<ArcDatum, "label" | "color">>;
    arcs: ArcDatum[];
    ribbons: RibbonDatum[];
};
export declare const useChordSelection: ({ arcOpacity, activeArcOpacity, inactiveArcOpacity, ribbons, ribbonOpacity, activeRibbonOpacity, inactiveRibbonOpacity, }: {
    arcOpacity?: number | undefined;
    activeArcOpacity?: number | undefined;
    inactiveArcOpacity?: number | undefined;
    ribbons: RibbonDatum[];
    ribbonOpacity?: number | undefined;
    activeRibbonOpacity?: number | undefined;
    inactiveRibbonOpacity?: number | undefined;
}) => {
    getArcOpacity: (arc: ArcDatum) => number;
    getRibbonOpacity: (ribbon: RibbonDatum) => number;
    selectedArcIds: string[];
    selectedRibbonIds: string[];
    currentArc: ArcDatum | null;
    setCurrentArc: import("react").Dispatch<import("react").SetStateAction<ArcDatum | null>>;
    currentRibbon: RibbonDatum | null;
    setCurrentRibbon: import("react").Dispatch<import("react").SetStateAction<RibbonDatum | null>>;
    hasSelection: boolean;
};
export declare const useCustomLayerProps: ({ center, radius, arcs, arcGenerator, ribbons, ribbonGenerator, }: {
    center: [number, number];
    radius: number;
    arcs: ArcDatum[];
    arcGenerator: any;
    ribbons: RibbonDatum[];
    ribbonGenerator: any;
}) => CustomLayerProps;
//# sourceMappingURL=hooks.d.ts.map