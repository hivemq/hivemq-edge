import { RGBColor } from 'd3-color';
import { Theme } from '@nivo/core';
export type ColorModifierBrightness = ['brighter', number];
export type ColorModifierDarkness = ['darker', number];
export type ColorModifierOpacity = ['opacity', number];
export type ColorModifier = ColorModifierBrightness | ColorModifierDarkness | ColorModifierOpacity;
export type ColorModifierFunction = (color: RGBColor) => RGBColor;
export type InheritedColorConfigStaticColor = string;
export type InheritedColorConfigCustomFunction<Datum> = (d: Datum, ...drest: Datum[]) => string;
export interface InheritedColorConfigFromTheme {
    theme: string;
}
export interface InheritedColorConfigFromContext {
    from: string;
    modifiers?: ColorModifier[];
}
export type InheritedColorConfig<Datum> = InheritedColorConfigStaticColor | InheritedColorConfigCustomFunction<Datum> | InheritedColorConfigFromTheme | InheritedColorConfigFromContext;
/**
 * Create a color generator for items which
 * might inherit from parent context,
 * for example labels, outlines…
 *
 * Support the following strategies:
 * - custom function
 * - color from theme
 * - color from parent, with optional color modifiers
 * - static color
 */
export declare const getInheritedColorGenerator: <Datum = any>(config: InheritedColorConfig<Datum>, theme?: Theme) => InheritedColorConfigCustomFunction<Datum> | ((d: Datum) => any);
export declare const useInheritedColor: <Datum = any>(config: InheritedColorConfig<Datum>, theme?: Theme) => InheritedColorConfigCustomFunction<Datum> | ((d: Datum) => any);
//# sourceMappingURL=inheritedColor.d.ts.map