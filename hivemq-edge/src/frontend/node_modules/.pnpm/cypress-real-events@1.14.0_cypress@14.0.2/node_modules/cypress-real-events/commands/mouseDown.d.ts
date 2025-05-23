import { ScrollBehaviorOptions, Position } from "../getCypressElementCoordinates";
import { mouseButtonNumbers } from "../mouseButtonNumbers";
export interface RealMouseDownOptions {
    /** Pointer type for realMouseDown, if "pen" touch simulated */
    pointer?: "mouse" | "pen";
    /**
     * Position of the realMouseDown event relative to the element
     * @example cy.realMouseDown({ position: "topLeft" })
     */
    position?: Position;
    /**
     * Controls how the page is scrolled to bring the subject into view, if needed.
     * @example cy.realMouseDown({ scrollBehavior: "top" });
     */
    scrollBehavior?: ScrollBehaviorOptions;
    /**
     * @default "left"
     */
    button?: keyof typeof mouseButtonNumbers;
    /**  X coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realMouseDown({ x: 100, y: 115 })
     * cy.get("body").realMouseDown({ x: 11, y: 12 }) // global click by coordinates
     */
    x?: number;
    /**  Y coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realMouseDown({ x: 100, y: 115 })
     * cy.get("body").realMouseDown({ x: 11, y: 12 }) // global click by coordinates
     */
    y?: number;
    /**
     * Indicates whether any modifier (shiftKey | altKey | ctrlKey | metaKey) was pressed or not when an event occurred
     * @example cy.realMouseDown({ shiftKey: true });
     */
    shiftKey?: boolean;
    altKey?: boolean;
    ctrlKey?: boolean;
    metaKey?: boolean;
    /**
     * The normalized pressure, which has a range of [0,1]. It affects the `pressure` property of the triggered
     * pointerdown event.
     *
     * @type {number}
     * @default {0.5}
     */
    pressure?: number;
}
/** @ignore this, update documentation for this function at index.d.ts */
export declare function realMouseDown(subject: JQuery, options?: RealMouseDownOptions): Promise<JQuery<HTMLElement>>;
