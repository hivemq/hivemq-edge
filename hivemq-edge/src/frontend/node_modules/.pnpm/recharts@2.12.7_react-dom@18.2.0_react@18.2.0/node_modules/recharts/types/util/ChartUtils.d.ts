import { Series } from 'victory-vendor/d3-shape';
import { ReactElement, ReactNode } from 'react';
import { AxisType, BaseAxisProps, DataKey, LayoutType, LegendType, PolarLayoutType, NumberDomain, TickItem, CategoricalDomain, StackOffsetType, Margin, ChartOffset } from './types';
import { getLegendProps } from './getLegendProps';
export { getLegendProps };
export declare function getValueByDataKey<T>(obj: T, dataKey: DataKey<T>, defaultValue?: any): any;
export declare function getDomainOfDataByKey<T>(data: Array<T>, key: DataKey<T>, type: BaseAxisProps['type'], filterNil?: boolean): NumberDomain | CategoricalDomain;
export declare const calculateActiveTickIndex: (coordinate: number, ticks?: Array<TickItem>, unsortedTicks?: Array<TickItem>, axis?: BaseAxisProps) => number;
export declare const getMainColorOfGraphicItem: (item: ReactElement) => any;
export interface FormattedGraphicalItem {
    props: {
        sectors?: ReadonlyArray<any>;
        data?: ReadonlyArray<any>;
    };
    childIndex: number;
    item: ReactElement<{
        legendType?: LegendType;
        hide: boolean;
        name?: string;
        dataKey: DataKey<any>;
    }>;
}
export type BarSetup = {
    barSize: number | string;
    stackList: ReadonlyArray<ReactElement>;
    item: ReactElement;
};
export declare const getBarSizeList: ({ barSize: globalSize, totalSize, stackGroups, }: {
    barSize: number | string;
    stackGroups: AxisStackGroups;
    totalSize: number;
}) => Record<string, ReadonlyArray<BarSetup>>;
export type BarPosition = {
    item: ReactElement;
    position: {
        offset: number;
        size: number | undefined | typeof NaN;
    };
};
export declare const getBarPosition: ({ barGap, barCategoryGap, bandSize, sizeList, maxBarSize, }: {
    barGap: string | number;
    barCategoryGap: string | number;
    bandSize: number;
    sizeList: ReadonlyArray<BarSetup>;
    maxBarSize: number;
}) => ReadonlyArray<BarPosition>;
export declare const appendOffsetOfLegend: (offset: ChartOffset, _unused: unknown, props: {
    width?: number;
    margin: Margin;
    children?: ReactNode[];
}, legendBox: DOMRect | null) => ChartOffset;
export declare const getDomainOfErrorBars: (data: Array<object>, item: ReactElement, dataKey: DataKey<any>, layout?: LayoutType, axisType?: AxisType) => NumberDomain | null;
export declare const parseErrorBarsOfAxis: (data: any[], items: any[], dataKey: any, axisType: AxisType, layout?: LayoutType) => NumberDomain | null;
export declare const getDomainOfItemsWithSameAxis: (data: any[], items: ReactElement[], type: BaseAxisProps['type'], layout?: LayoutType, filterNil?: boolean) => NumberDomain | CategoricalDomain;
export declare const isCategoricalAxis: (layout: LayoutType | PolarLayoutType, axisType: AxisType) => boolean;
export declare const getCoordinatesOfGrid: (ticks: Array<TickItem>, minValue: number, maxValue: number, syncWithTicks: Boolean) => number[];
export declare const getTicksOfAxis: (axis: BaseAxisProps & {
    duplicateDomain?: any;
    realScaleType?: 'scaleBand' | 'band' | 'point' | 'linear';
    scale?: any;
    axisType?: AxisType;
    ticks?: any;
    niceTicks?: any;
    isCategorical?: boolean;
    categoricalDomain?: any;
}, isGrid?: boolean, isAll?: boolean) => TickItem[] | null;
export declare const combineEventHandlers: (defaultHandler: Function, childHandler: Function | undefined) => Function;
export declare const parseScale: (axis: {
    scale: 'auto' | string | Function;
    type?: BaseAxisProps['type'];
    layout?: 'radial' | unknown;
    axisType?: 'radiusAxis' | 'angleAxis' | unknown;
}, chartType?: string, hasBar?: boolean) => {
    scale: any;
    realScaleType?: string;
};
export declare const checkDomainOfScale: (scale: any) => void;
export declare const findPositionOfBar: (barPosition: any[], child: ReactNode) => any;
export declare const truncateByDomain: (value: [number, number], domain: number[]) => number[];
export declare const offsetSign: OffsetAccessor;
export declare const offsetPositive: OffsetAccessor;
type OffsetAccessor = (series: Array<Series<Record<string, unknown>, string>>, order: number[]) => void;
export declare const getStackedData: (data: ReadonlyArray<Record<string, unknown>>, stackItems: ReadonlyArray<{
    props: {
        dataKey?: DataKey<any>;
    };
}>, offsetType: StackOffsetType) => ReadonlyArray<Series<Record<string, unknown>, string>>;
type AxisId = string;
export type StackId = string | number | symbol;
export type ParentStackGroup = {
    hasStack: boolean;
    stackGroups: Record<StackId, ChildStackGroup>;
};
export type GenericChildStackGroup<T> = {
    numericAxisId: string;
    cateAxisId: string;
    items: Array<ReactElement>;
    stackedData?: ReadonlyArray<T>;
};
export type ChildStackGroup = GenericChildStackGroup<Series<Record<string, unknown>, string>>;
export type AxisStackGroups = Record<AxisId, ParentStackGroup>;
export declare const getStackGroupsByAxisId: (data: ReadonlyArray<Record<string, unknown>> | undefined, _items: Array<ReactElement>, numericAxisId: string, cateAxisId: string, offsetType: StackOffsetType, reverseStackOrder: boolean) => AxisStackGroups;
export declare const getTicksOfScale: (scale: any, opts: any) => {
    niceTicks: any;
};
export declare function getCateCoordinateOfLine<T extends Record<string, unknown>>({ axis, ticks, bandSize, entry, index, dataKey, }: {
    axis: {
        dataKey?: DataKey<T>;
        allowDuplicatedCategory?: boolean;
        type?: BaseAxisProps['type'];
        scale: (v: number) => number;
    };
    ticks: Array<TickItem>;
    bandSize: number;
    entry: T;
    index: number;
    dataKey?: DataKey<T>;
}): number | null;
export declare const getCateCoordinateOfBar: ({ axis, ticks, offset, bandSize, entry, index, }: {
    axis: any;
    ticks: Array<TickItem>;
    offset: any;
    bandSize: number;
    entry: any;
    index: number;
}) => any;
export declare const getBaseValueOfBar: ({ numericAxis, }: {
    numericAxis: any;
}) => any;
export declare const getStackedDataOfItem: <StackedData>(item: ReactElement, stackGroups: Record<StackId, GenericChildStackGroup<StackedData>>) => StackedData;
export declare const getDomainOfStackGroups: (stackGroups: Record<StackId, ChildStackGroup>, startIndex: number, endIndex: number) => number[];
export declare const MIN_VALUE_REG: RegExp;
export declare const MAX_VALUE_REG: RegExp;
export declare const parseSpecifiedDomain: (specifiedDomain: any, dataDomain: any, allowDataOverflow?: boolean) => any;
export declare const getBandSizeOfAxis: (axis?: BaseAxisProps, ticks?: Array<TickItem>, isBar?: boolean) => number | undefined;
export declare const parseDomainOfCategoryAxis: <T>(specifiedDomain: readonly T[], calculatedDomain: readonly T[], axisChild: ReactElement) => readonly T[];
export declare const getTooltipItem: (graphicalItem: ReactElement, payload: any) => {
    dataKey: any;
    unit: any;
    formatter: any;
    name: any;
    color: any;
    value: any;
    type: any;
    payload: any;
    chartType: any;
    hide: any;
};
