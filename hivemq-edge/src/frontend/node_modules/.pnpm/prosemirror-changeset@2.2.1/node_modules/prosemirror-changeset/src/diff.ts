import {Fragment} from "prosemirror-model"
import {Change} from "./change"

// Convert the given range of a fragment to tokens, where node open
// tokens are encoded as strings holding the node name, characters as
// their character code, and node close tokens as -1.
function tokens(frag: Fragment, start: number, end: number, target: (number | string)[]) {
  for (let i = 0, off = 0; i < frag.childCount; i++) {
    let child = frag.child(i), endOff = off + child.nodeSize
    let from = Math.max(off, start), to = Math.min(endOff, end)
    if (from < to) {
      if (child.isText) {
        for (let j = from; j < to; j++) target.push(child.text!.charCodeAt(j - off))
      } else if (child.isLeaf) {
        target.push(child.type.name)
      } else {
        if (from == off) target.push(child.type.name)
        tokens(child.content, Math.max(off + 1, from) - off - 1, Math.min(endOff - 1, to) - off - 1, target)
        if (to == endOff) target.push(-1)
      }
    }
    off = endOff
  }
  return target
}

// The code below will refuse to compute a diff with more than 5000
// insertions or deletions, which takes about 300ms to reach on my
// machine. This is a safeguard against runaway computations.
const MAX_DIFF_SIZE = 5000

// This obscure mess of constants computes the minimum length of an
// unchanged range (not at the start/end of the compared content). The
// idea is to make it higher in bigger replacements, so that you don't
// get a diff soup of coincidentally identical letters when replacing
// a paragraph.
function minUnchanged(sizeA: number, sizeB: number) {
  return Math.min(15, Math.max(2, Math.floor(Math.max(sizeA, sizeB) / 10)))
}

export function computeDiff(fragA: Fragment, fragB: Fragment, range: Change) {
  let tokA = tokens(fragA, range.fromA, range.toA, [])
  let tokB = tokens(fragB, range.fromB, range.toB, [])

  // Scan from both sides to cheaply eliminate work
  let start = 0, endA = tokA.length, endB = tokB.length
  while (start < tokA.length && start < tokB.length && tokA[start] === tokB[start]) start++
  if (start == tokA.length && start == tokB.length) return []
  while (endA > start && endB > start && tokA[endA - 1] === tokB[endB - 1]) endA--, endB--
  // If the result is simple _or_ too big to cheaply compute, return
  // the remaining region as the diff
  if (endA == start || endB == start || (endA == endB && endA == start + 1))
    return [range.slice(start, endA, start, endB)]

  // This is an implementation of Myers' diff algorithm
  // See https://neil.fraser.name/writing/diff/myers.pdf and
  // https://blog.jcoglan.com/2017/02/12/the-myers-diff-algorithm-part-1/

  let lenA = endA - start, lenB = endB - start
  let max = Math.min(MAX_DIFF_SIZE, lenA + lenB), off = max + 1
  let history: number[][] = []
  let frontier: number[] = []
  for (let len = off * 2, i = 0; i < len; i++) frontier[i] = -1

  for (let size = 0; size <= max; size++) {
    for (let diag = -size; diag <= size; diag += 2) {
      let next = frontier[diag + 1 + max], prev = frontier[diag - 1 + max]
      let x = next < prev ? prev : next + 1, y = x + diag
      while (x < lenA && y < lenB && tokA[start + x] === tokB[start + y]) x++, y++
      frontier[diag + max] = x
      // Found a match
      if (x >= lenA && y >= lenB) {
        // Trace back through the history to build up a set of changed ranges.
        let diff = [], minSpan = minUnchanged(endA - start, endB - start)
        // Used to add steps to a diff one at a time, back to front, merging
        // ones that are less than minSpan tokens apart
        let fromA = -1, toA = -1, fromB = -1, toB = -1
        let add = (fA: number, tA: number, fB: number, tB: number) => {
          if (fromA > -1 && fromA < tA + minSpan) {
            fromA = fA; fromB = fB
          } else {
            if (fromA > -1)
              diff.push(range.slice(fromA, toA, fromB, toB))
            fromA = fA; toA = tA
            fromB = fB; toB = tB
          }
        }

        for (let i = size - 1; i >= 0; i--) {
          let next = frontier[diag + 1 + max], prev = frontier[diag - 1 + max]
          if (next < prev) { // Deletion
            diag--
            x = prev + start; y = x + diag
            add(x, x, y, y + 1)
          } else { // Insertion
            diag++
            x = next + start; y = x + diag
            add(x, x + 1, y, y)
          }
          frontier = history[i >> 1]
        }
        if (fromA > -1) diff.push(range.slice(fromA, toA, fromB, toB))
        return diff.reverse()
      }
    }
    // Since only either odd or even diagonals are read from each
    // frontier, we only copy them every other iteration.
    if (size % 2 == 0) history.push(frontier.slice())
  }
  // The loop exited, meaning the maximum amount of work was done.
  // Just return a change spanning the entire range.
  return [range.slice(start, endA, start, endB)]
}
