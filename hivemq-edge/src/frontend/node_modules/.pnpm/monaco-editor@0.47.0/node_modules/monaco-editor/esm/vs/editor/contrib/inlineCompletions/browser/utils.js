import { BugIndicatingError } from '../../../../base/common/errors.js';
import { DisposableStore } from '../../../../base/common/lifecycle.js';
import { autorunOpts } from '../../../../base/common/observable.js';
import { Position } from '../../../common/core/position.js';
import { Range } from '../../../common/core/range.js';
export function applyEdits(text, edits) {
    const transformer = new PositionOffsetTransformer(text);
    const offsetEdits = edits.map(e => {
        const range = Range.lift(e.range);
        return ({
            startOffset: transformer.getOffset(range.getStartPosition()),
            endOffset: transformer.getOffset(range.getEndPosition()),
            text: e.text
        });
    });
    offsetEdits.sort((a, b) => b.startOffset - a.startOffset);
    for (const edit of offsetEdits) {
        text = text.substring(0, edit.startOffset) + edit.text + text.substring(edit.endOffset);
    }
    return text;
}
class PositionOffsetTransformer {
    constructor(text) {
        this.lineStartOffsetByLineIdx = [];
        this.lineStartOffsetByLineIdx.push(0);
        for (let i = 0; i < text.length; i++) {
            if (text.charAt(i) === '\n') {
                this.lineStartOffsetByLineIdx.push(i + 1);
            }
        }
    }
    getOffset(position) {
        return this.lineStartOffsetByLineIdx[position.lineNumber - 1] + position.column - 1;
    }
}
const array = [];
export function getReadonlyEmptyArray() {
    return array;
}
export class ColumnRange {
    constructor(startColumn, endColumnExclusive) {
        this.startColumn = startColumn;
        this.endColumnExclusive = endColumnExclusive;
        if (startColumn > endColumnExclusive) {
            throw new BugIndicatingError(`startColumn ${startColumn} cannot be after endColumnExclusive ${endColumnExclusive}`);
        }
    }
    toRange(lineNumber) {
        return new Range(lineNumber, this.startColumn, lineNumber, this.endColumnExclusive);
    }
    equals(other) {
        return this.startColumn === other.startColumn
            && this.endColumnExclusive === other.endColumnExclusive;
    }
}
export function applyObservableDecorations(editor, decorations) {
    const d = new DisposableStore();
    const decorationsCollection = editor.createDecorationsCollection();
    d.add(autorunOpts({ debugName: () => `Apply decorations from ${decorations.debugName}` }, reader => {
        const d = decorations.read(reader);
        decorationsCollection.set(d);
    }));
    d.add({
        dispose: () => {
            decorationsCollection.clear();
        }
    });
    return d;
}
export function addPositions(pos1, pos2) {
    return new Position(pos1.lineNumber + pos2.lineNumber - 1, pos2.lineNumber === 1 ? pos1.column + pos2.column - 1 : pos2.column);
}
export function subtractPositions(pos1, pos2) {
    return new Position(pos1.lineNumber - pos2.lineNumber + 1, pos1.lineNumber - pos2.lineNumber === 0 ? pos1.column - pos2.column + 1 : pos1.column);
}
export function lengthOfText(text) {
    let line = 1;
    let column = 1;
    for (const c of text) {
        if (c === '\n') {
            line++;
            column = 1;
        }
        else {
            column++;
        }
    }
    return new Position(line, column);
}
/**
 * Given some text edits, this function finds the new ranges of the editted text post application of all edits.
 * Assumes that the edit ranges are disjoint and they are sorted in the order of the ranges
 * @param edits edits applied
 * @returns new ranges post edits for every edit
 */
export function getNewRanges(edits) {
    var _a;
    const newRanges = [];
    let previousEditEndLineNumber = 0;
    let lineOffset = 0;
    let columnOffset = 0;
    for (const edit of edits) {
        const text = (_a = edit.text) !== null && _a !== void 0 ? _a : '';
        const textLength = lengthOfText(text);
        const newRangeStart = Position.lift({
            lineNumber: edit.range.startLineNumber + lineOffset,
            column: edit.range.startColumn + (edit.range.startLineNumber === previousEditEndLineNumber ? columnOffset : 0)
        });
        const newRangeEnd = addPositions(newRangeStart, textLength);
        newRanges.push(Range.fromPositions(newRangeStart, newRangeEnd));
        lineOffset += textLength.lineNumber - edit.range.endLineNumber + edit.range.startLineNumber - 1;
        columnOffset = newRangeEnd.column - edit.range.endColumn;
        previousEditEndLineNumber = edit.range.endLineNumber;
    }
    return newRanges;
}
export class Permutation {
    constructor(_indexMap) {
        this._indexMap = _indexMap;
    }
    static createSortPermutation(arr, compareFn) {
        const sortIndices = Array.from(arr.keys()).sort((index1, index2) => compareFn(arr[index1], arr[index2]));
        return new Permutation(sortIndices);
    }
    apply(arr) {
        return arr.map((_, index) => arr[this._indexMap[index]]);
    }
    inverse() {
        const inverseIndexMap = this._indexMap.slice();
        for (let i = 0; i < this._indexMap.length; i++) {
            inverseIndexMap[this._indexMap[i]] = i;
        }
        return new Permutation(inverseIndexMap);
    }
}
