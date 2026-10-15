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
 * The tag exists, and the node it names cannot be resolved on the device — it is absent from the address space,
 * or it is of a kind the tag's purpose does not allow, or its id is malformed. Only the adapter can say which, so
 * its own sentence is the detail.
 * <p>
 * <b>Distinct from {@link DomainTagNotFoundError}, and the distinction is the whole point.</b> That one answers a
 * lookup by name that found no tag, where the name is the whole story. Here the tag was found — an operator may
 * have created it a moment ago and can see it listed — and what could not be found is one level deeper. Reporting
 * that as "No tag found with the provided id" sends the reader looking for something that is in front of them,
 * which is what QA reported as P17 on EDG-1116.
 * <p>
 * Still {@code 404}: something genuinely could not be found, so the status was never the part that misled. A
 * separate type rather than a configurable title on the existing one, because the three honest uses of that type
 * would then be free to drift, and a typed error whose title varies is no longer a type.
 */
public class DomainTagNodeNotResolvableError extends ProblemDetails {
    public DomainTagNodeNotResolvableError(final @NotNull String reason) {
        super(
                "TagNodeNotResolvable",
                "The tag's node could not be resolved on the device",
                reason,
                HttpStatus.NOT_FOUND_404,
                List.of());
    }
}
