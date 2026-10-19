/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A tag's schema could not be resolved. Two situations reach this, and the adapter's own sentence is the detail
 * because only the adapter can say which:
 * <ul>
 *   <li>the tag exists and the node it names cannot be resolved on the device — absent from the address space, of
 *       a kind the tag's purpose does not allow, or a malformed id;</li>
 *   <li>the tag does not exist at all.</li>
 * </ul>
 * <b>The title names neither case, deliberately.</b> It said "The tag's node could not be resolved on the device",
 * which asserts the tag exists — true in the first situation and misleading in the second, where it sends an
 * operator to the device to look for a tag that was never created. Both situations arrive through one adapter
 * callback, {@code tagNotFound(String)}, carrying free text and nothing else, so the title cannot tell them apart
 * and must not pretend to. Telling them apart needs a second callback, which is SDK surface; inferring the case by
 * parsing the adapter's English sentence would be worse than a neutral title.
 * <p>
 * <b>Distinct from {@link DomainTagNotFoundError}.</b> That one answers a lookup by name where the name is the
 * whole story. This one answers a schema request, where the tag may well be listed in front of the reader and the
 * failure is one level deeper. Reporting that as "No tag found with the provided id" was QA's P17 on EDG-1116.
 * <p>
 * Still {@code 404}: something genuinely could not be found, so the status was never the part that misled. A
 * separate type rather than a configurable title on the existing one, because the three honest uses of that type
 * would then be free to drift, and a typed error whose title varies is no longer a type.
 * <p>
 * Reported by Sam as V11 in his fifth review of EDG-1087, with the neutral title as his own suggestion.
 */
public class DomainTagNodeNotResolvableError extends ProblemDetails {
    public DomainTagNodeNotResolvableError(final @NotNull String reason) {
        super(
                "TagNodeNotResolvable",
                "The tag schema could not be resolved",
                reason,
                HttpStatus.NOT_FOUND_404,
                List.of());
    }
}
