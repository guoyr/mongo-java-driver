/*
 *
 *  * Copyright (c) 2008-2014 MongoDB, Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.bson;

/**
 * The representation to use when converting a UUID to a BSON binary value.
 * This class is necessary because the different drivers used to have different
 * ways of encoding UUID, with the BSON subtype: \x03 UUID old.
 *
 * For empty collections, please use the STANDARD representation, which is
 * the same across all drivers and have the BSON subtype: \x04 UUID
 *
 * @since 3.0
 */
public enum UuidRepresentation {
    /**
     * the canonical representation of UUID, use it for any new collections
     */
    STANDARD,

    /**
     * The legacy representation of UUID used by the C# driver
     */
    C_SHARP_LEGACY,

    /**
     * The legacy representation of UUID used by the Java driver
     */
    JAVA_LEGACY,

    /**
     * The legacy representation of UUID used by Python, which is the same
     * format as STANDARD, but has the UUID old BSON subtype (\x03)
     */
    PYTHON_LEGACY
}
