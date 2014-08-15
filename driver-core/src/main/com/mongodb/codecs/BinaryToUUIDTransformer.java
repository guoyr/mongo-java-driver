/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.codecs;

import org.bson.BSONException;
import org.bson.BsonBinary;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonSerializationException;
import org.bson.UuidRepresentation;

import java.util.UUID;

/**
 * A transformer from {@code BsonBinary} to {@code UUID}.
 *
 * @since 3.0
 */
public class BinaryToUUIDTransformer implements BinaryTransformer<UUID> {

    private UuidRepresentation uuidRepresentation;

    public BinaryToUUIDTransformer() {
    }

    public BinaryToUUIDTransformer(final UuidRepresentation uuidRepresentation) {
        this.uuidRepresentation = uuidRepresentation;
    }

    @Override
    public UUID transform(final BsonBinary binary) {

        byte[] binaryData = binary.getData();

        if (binaryData.length != 16) {
            throw new BsonSerializationException(String.format("Expected length to be 16, not %d.", binaryData.length));
        }

        switch (uuidRepresentation) {
            case C_SHARP_LEGACY:
                reverse(binaryData, 0, 4);
                reverse(binaryData, 4, 2);
                reverse(binaryData, 6, 2);
                break;
            case JAVA_LEGACY:
                reverse(binaryData, 0, 8);
                reverse(binaryData, 8, 8);
                break;
            case PYTHON_LEGACY:
            case STANDARD:
                break;
            case UNSPECIFIED:
                throw new BsonInvalidOperationException(
                        "Unable to convert byte array to Guid because GuidRepresentation is Unspecified.");
            default:
                throw new BSONException("Unexpected UUID representation");

        }

        return new UUID(readLongFromArrayBigEndian(binaryData, 0), readLongFromArrayBigEndian(binaryData, 8));
    }

    // reverse elements in the subarray data[i:i1]
    private void reverse(final byte[] data, final int start, final int length) {
        for (int left = start, right = start + length - 1; left < right; left++, right--) {
            // swap the values at the left and right indices
            byte temp = data[left];
            data[left]  = data[right];
            data[right] = temp;
        }
    }

    private static long readLongFromArrayBigEndian(final byte[] bytes, final int offset) {
        long x = 0;
        x |= (0xFFL & bytes[offset + 7]);
        x |= (0xFFL & bytes[offset + 6]) << 8;
        x |= (0xFFL & bytes[offset + 5]) << 16;
        x |= (0xFFL & bytes[offset + 4]) << 24;
        x |= (0xFFL & bytes[offset + 3]) << 32;
        x |= (0xFFL & bytes[offset + 2]) << 40;
        x |= (0xFFL & bytes[offset + 1]) << 48;
        x |= (0xFFL & bytes[offset]) << 56;
        return x;
    }
}