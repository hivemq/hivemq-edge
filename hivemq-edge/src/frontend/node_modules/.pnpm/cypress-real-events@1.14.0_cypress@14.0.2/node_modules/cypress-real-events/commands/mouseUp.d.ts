import { ScrollBehaviorOptions, Position } from "../getCypressElementCoordinates";
import { mouseButtonNumbers } from "../mouseButtonNumbers";
export interface RealMouseUpOptions {
    /** Pointer type for realMouseUp, if "pen" touch simulated */
    pointer?: "mouse" | "pen";
    /**
     * Position of the realMouseUp event relative to the element
     * @example cy.realMouseUp({ position: "topLeft" })
     */
    position?: Position;
    /**
     * Controls how the page is scrolled to bring the subject into view, if needed.
     * @example cy.realMouseUp({ scrollBehavior: "top" });
     */
    scrollBehavior?: ScrollBehaviorOptions;
    /**
     * @default "left"
     */
    button?: keyof typeof mouseButtonNumbers;
    /**  X coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realMouseUp({ x: 100, y: 115 })
     * cy.get("body").realMouseUp({ x: 11, y: 12 }) // global click by coordinates
     */
    x?: number;
    /**  Y coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realMouseUp({ x: 100, y: 115 })
     * cy.get("body").realMouseUp({ x: 11, y: 12 }) // global click by coordinates
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
}
/** @ignore this, update documentation for this function at index.d.ts */
export declare function realMouseUp(subject: JQuery, options?: RealMouseUpOptions): Promise<JQuery<HTMLElement>>;
