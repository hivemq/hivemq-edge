package com.hivemq.licensethirdparty

interface License {
    val fullName: String
    val url: String?
}

enum class KnownLicense(val id: String, override val fullName: String, override val url: String) : License {
    APACHE_2_0("Apache-2.0", "Apache License 2.0", "https://spdx.org/licenses/Apache-2.0.html"),
    BLUE_OAK_1_0_0("BlueOak-1.0.0", "Blue Oak Model License 1.0.0", "https://spdx.org/licenses/BlueOak-1.0.0.html"),
    BOUNCY_CASTLE("MIT", "Bouncy Castle Licence", "https://www.bouncycastle.org/licence.html"),
    BSD_2_CLAUSE("BSD-2-Clause", "BSD 2-Clause \"Simplified\" License", "https://spdx.org/licenses/BSD-2-Clause.html"),
    BSD_3_CLAUSE("BSD-3-Clause", "BSD 3-Clause \"New\" or \"Revised\" License", "https://spdx.org/licenses/BSD-3-Clause.html"),
    CC_BY_4_0("CC-BY-4.0", "Creative Commons Attribution 4.0 International", "https://spdx.org/licenses/CC-BY-4.0.html"),
    CC0_1_0("CC0-1.0", "Creative Commons Zero v1.0 Universal", "https://spdx.org/licenses/CC0-1.0.html"),
    CDDL_1_0("CDDL-1.0", "Common Development and Distribution License 1.0", "https://spdx.org/licenses/CDDL-1.0.html"),
    CDDL_1_1("CDDL-1.1", "Common Development and Distribution License 1.1", "https://spdx.org/licenses/CDDL-1.1.html"),
    // EDL has BSD-3-Clause as SPDX id, documented in the following links:
    // https://spdx.org/licenses/BSD-3-Clause.html
    // https://www.eclipse.org/org/documents/edl-v10.php
    // https://lists.spdx.org/g/Spdx-legal/topic/request_for_adding_eclipse/67981884
    EDL_1_0("BSD-3-Clause", "Eclipse Distribution License - v 1.0", "https://www.eclipse.org/org/documents/edl-v10.php"),
    EPL_1_0("EPL-1.0", "Eclipse Public License 1.0", "https://spdx.org/licenses/EPL-1.0.html"),
    EPL_2_0("EPL-2.0", "Eclipse Public License 2.0", "https://spdx.org/licenses/EPL-2.0.html"),
    GO("BSD-3-Clause", "Go License", "https://golang.org/LICENSE"),
    ISC("ISC", "ISC License", "https://spdx.org/licenses/ISC.html"),
    LGPL_2_1_OR_LATER("LGPL-2.1-or-later", "GNU Lesser General Public License v2.1 or later", "https://spdx.org/licenses/LGPL-2.1-or-later.html"),
    MIT("MIT", "MIT License", "https://spdx.org/licenses/MIT.html"),
    MIT_0("MIT-0", "MIT No Attribution", "https://spdx.org/licenses/MIT-0.html"),
    OFL_1_1("OFL-1.1", "SIL Open Font License 1.1", "https://spdx.org/licenses/OFL-1.1.html"),
    PUBLIC_DOMAIN("Public Domain", "Public Domain", ""),
    UNICODE_DFS_2016("Unicode-DFS-2016", "Unicode License Agreement - Data Files and Software (2016)", "https://spdx.org/licenses/Unicode-DFS-2016.html"),
    UNLICENSE("Unlicense", "Unlicense Yourself: Set Your Code Free", "https://unlicense.org/"),
    W3C_19980720("W3C-19980720", "W3C Software Notice and License (1998-07-20)", "https://spdx.org/licenses/W3C-19980720.html"),
    ZERO_BSD("0BSD", "BSD Zero Clause License", "https://spdx.org/licenses/0BSD.html"),
}


data class UnknownLicense(override val fullName: String, override val url: String?) : License
