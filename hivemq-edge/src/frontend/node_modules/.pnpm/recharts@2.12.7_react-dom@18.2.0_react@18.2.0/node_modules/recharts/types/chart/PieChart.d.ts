export declare const PieChart: {
    new (props: import("./generateCategoricalChart").CategoricalChartProps): {
        readonly eventEmitterSymbol: Symbol;
        clipPathId: string;
        accessibilityManager: import("./AccessibilityManager").AccessibilityManager;
        throttleTriggeredAfterMouseMove: import("lodash").DebouncedFunc<(e: import("./generateCategoricalChart").MousePointer) => any>;
        container?: HTMLElement;
        componentDidMount(): void;
        displayDefaultTooltip(): void;
        getSnapshotBeforeUpdate(prevProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, prevState: Readonly<import("./types").CategoricalChartState>): null;
        componentDidUpdate(prevProps: import("./generateCategoricalChart").CategoricalChartProps): void;
        componentWillUnmount(): void;
        getTooltipEventType(): import("../util/types").TooltipEventType;
        getMouseInfo(event: import("./generateCategoricalChart").MousePointer): {
            xValue: any;
            yValue: any;
            chartX: number;
            chartY: number;
        } | {
            activeTooltipIndex: number;
            activeLabel: any;
            activePayload: any[];
            activeCoordinate: import("../util/types").ChartCoordinate;
            chartX: number;
            chartY: number;
        };
        inRange(x: number, y: number, scale?: number): any;
        parseEventsOfWrapper(): any;
        addListener(): void;
        removeListener(): void;
        handleLegendBBoxUpdate: (box: DOMRect) => void;
        handleReceiveSyncEvent: (cId: string | number, data: import("./types").CategoricalChartState, emitter: Symbol) => void;
        handleBrushChange: ({ startIndex, endIndex }: {
            startIndex: number;
            endIndex: number;
        }) => void;
        handleMouseEnter: (e: import("react").MouseEvent<Element, MouseEvent>) => void;
        triggeredAfterMouseMove: (e: import("./generateCategoricalChart").MousePointer) => any;
        handleItemMouseEnter: (el: any) => void;
        handleItemMouseLeave: () => void;
        handleMouseMove: (e: import("./generateCategoricalChart").MousePointer & Partial<Omit<import("react").MouseEvent<Element, MouseEvent>, keyof import("./generateCategoricalChart").MousePointer>>) => void;
        handleMouseLeave: (e: any) => void;
        handleOuterEvent: (e: import("react").MouseEvent<Element, MouseEvent> | import("react").TouchEvent<Element>) => void;
        handleClick: (e: import("react").MouseEvent<Element, MouseEvent>) => void;
        handleMouseDown: (e: import("react").MouseEvent<Element, MouseEvent> | import("react").Touch) => void;
        handleMouseUp: (e: import("react").MouseEvent<Element, MouseEvent> | import("react").Touch) => void;
        handleTouchMove: (e: import("react").TouchEvent<Element>) => void;
        handleTouchStart: (e: import("react").TouchEvent<Element>) => void;
        handleTouchEnd: (e: import("react").TouchEvent<Element>) => void;
        triggerSyncEvent: (data: import("./types").CategoricalChartState) => void;
        applySyncEvent: (data: import("./types").CategoricalChartState) => void;
        filterFormatItem(item: any, displayName: any, childIndex: any): any;
        renderCursor: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").JSX.Element;
        renderPolarAxis: (element: any, displayName: string, index: number) => import("react").DetailedReactHTMLElement<{
            className: string;
            key: any;
            ticks: import("../util/types").TickItem[];
            type?: "number" | "category";
            dataKey?: import("../util/types").DataKey<any>;
            hide?: boolean;
            scale?: Function | import("../util/types").ScaleType;
            tick?: boolean | import("react").SVGProps<SVGTextElement> | import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>> | ((props: any) => import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>>);
            tickCount?: number;
            axisLine?: boolean | import("react").SVGProps<SVGLineElement>;
            tickLine?: boolean | import("react").SVGProps<SVGTextElement>;
            tickSize?: number;
            tickFormatter?: (value: any, index: number) => string;
            allowDataOverflow?: boolean;
            allowDuplicatedCategory?: boolean;
            allowDecimals?: boolean;
            domain?: import("../util/types").AxisDomain;
            includeHidden?: boolean;
            name?: string;
            unit?: string | number;
            axisType?: import("../util/types").AxisType;
            range?: number[];
            AxisComp?: any;
            label?: string | number | object | import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        }, HTMLElement>;
        renderPolarGrid: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderLegend: () => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderTooltip: () => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderBrush: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderReferenceElement: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderActivePoints: ({ item, activePoint, basePoint, childIndex, isRange }: any) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>[];
        renderGraphicChild: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
        renderCustomized: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
        renderClipPath(): import("react").JSX.Element;
        getXScales(): {
            [x: string]: Function | import("../util/types").ScaleType;
        };
        getYScales(): {
            [x: string]: Function | import("../util/types").ScaleType;
        };
        getXScaleByAxisId(axisId: string): Function | import("../util/types").ScaleType;
        getYScaleByAxisId(axisId: string): Function | import("../util/types").ScaleType;
        getItemByXY(chartXY: {
            x: number;
            y: number;
        }): {
            graphicalItem: any;
            payload: any;
        };
        renderMap: {
            CartesianGrid: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
                once: boolean;
            };
            ReferenceArea: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
            ReferenceLine: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
            ReferenceDot: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
            XAxis: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
            YAxis: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
            Brush: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
                once: boolean;
            };
            Bar: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Line: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Area: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Radar: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            RadialBar: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Scatter: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Pie: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Funnel: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => any[];
            };
            Tooltip: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").JSX.Element;
                once: boolean;
            };
            PolarGrid: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
                once: boolean;
            };
            PolarAngleAxis: {
                handler: (element: any, displayName: string, index: number) => import("react").DetailedReactHTMLElement<{
                    className: string;
                    key: any;
                    ticks: import("../util/types").TickItem[];
                    type?: "number" | "category";
                    dataKey?: import("../util/types").DataKey<any>;
                    hide?: boolean;
                    scale?: Function | import("../util/types").ScaleType;
                    tick?: boolean | import("react").SVGProps<SVGTextElement> | import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>> | ((props: any) => import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>>);
                    tickCount?: number;
                    axisLine?: boolean | import("react").SVGProps<SVGLineElement>;
                    tickLine?: boolean | import("react").SVGProps<SVGTextElement>;
                    tickSize?: number;
                    tickFormatter?: (value: any, index: number) => string;
                    allowDataOverflow?: boolean;
                    allowDuplicatedCategory?: boolean;
                    allowDecimals?: boolean;
                    domain?: import("../util/types").AxisDomain;
                    includeHidden?: boolean;
                    name?: string;
                    unit?: string | number;
                    axisType?: import("../util/types").AxisType;
                    range?: number[];
                    AxisComp?: any;
                    label?: string | number | object | import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
                }, HTMLElement>;
            };
            PolarRadiusAxis: {
                handler: (element: any, displayName: string, index: number) => import("react").DetailedReactHTMLElement<{
                    className: string;
                    key: any;
                    ticks: import("../util/types").TickItem[];
                    type?: "number" | "category";
                    dataKey?: import("../util/types").DataKey<any>;
                    hide?: boolean;
                    scale?: Function | import("../util/types").ScaleType;
                    tick?: boolean | import("react").SVGProps<SVGTextElement> | import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>> | ((props: any) => import("react").ReactElement<SVGElement, string | import("react").JSXElementConstructor<any>>);
                    tickCount?: number;
                    axisLine?: boolean | import("react").SVGProps<SVGLineElement>;
                    tickLine?: boolean | import("react").SVGProps<SVGTextElement>;
                    tickSize?: number;
                    tickFormatter?: (value: any, index: number) => string;
                    allowDataOverflow?: boolean;
                    allowDuplicatedCategory?: boolean;
                    allowDecimals?: boolean;
                    domain?: import("../util/types").AxisDomain;
                    includeHidden?: boolean;
                    name?: string;
                    unit?: string | number;
                    axisType?: import("../util/types").AxisType;
                    range?: number[];
                    AxisComp?: any;
                    label?: string | number | object | import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
                }, HTMLElement>;
            };
            Customized: {
                handler: (element: import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>, displayName: string, index: number) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
            };
        };
        render(): import("react").JSX.Element;
        context: any;
        setState<K extends keyof import("./types").CategoricalChartState>(state: import("./types").CategoricalChartState | ((prevState: Readonly<import("./types").CategoricalChartState>, props: Readonly<import("./generateCategoricalChart").CategoricalChartProps>) => import("./types").CategoricalChartState | Pick<import("./types").CategoricalChartState, K>) | Pick<import("./types").CategoricalChartState, K>, callback?: () => void): void;
        forceUpdate(callback?: () => void): void;
        readonly props: Readonly<import("./generateCategoricalChart").CategoricalChartProps> & Readonly<{
            children?: import("react").ReactNode;
        }>;
        state: Readonly<import("./types").CategoricalChartState>;
        refs: {
            [key: string]: import("react").ReactInstance;
        };
        shouldComponentUpdate?(nextProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, nextState: Readonly<import("./types").CategoricalChartState>, nextContext: any): boolean;
        componentDidCatch?(error: Error, errorInfo: import("react").ErrorInfo): void;
        componentWillMount?(): void;
        UNSAFE_componentWillMount?(): void;
        componentWillReceiveProps?(nextProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, nextContext: any): void;
        UNSAFE_componentWillReceiveProps?(nextProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, nextContext: any): void;
        componentWillUpdate?(nextProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, nextState: Readonly<import("./types").CategoricalChartState>, nextContext: any): void;
        UNSAFE_componentWillUpdate?(nextProps: Readonly<import("./generateCategoricalChart").CategoricalChartProps>, nextState: Readonly<import("./types").CategoricalChartState>, nextContext: any): void;
    };
    displayName: string;
    defaultProps: import("./generateCategoricalChart").CategoricalChartProps;
    getDerivedStateFromProps: (nextProps: import("./generateCategoricalChart").CategoricalChartProps, prevState: import("./types").CategoricalChartState) => import("./types").CategoricalChartState;
    renderActiveDot: (option: any, props: any) => import("react").ReactElement<any, string | import("react").JSXElementConstructor<any>>;
    contextType?: import("react").Context<any>;
};
