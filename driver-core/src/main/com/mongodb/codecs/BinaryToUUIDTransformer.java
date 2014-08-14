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
import org.bson.codecs.DecoderContext;

import java.util.UUID;

/**
 * A transformer from {@code BsonBinary} to {@code UUID}.
 *
 * @since 3.0
 */
public class BinaryToUUIDTransformer implements BinaryTransformer<UUID> {

    private DecoderContext decoderContext;

    public DecoderContext getDecoderContext() {
        return decoderContext;
    }

    public void setDecoderContext(DecoderContext decoderContext) {
        this.decoderContext = decoderContext;
    }

    @Override
    public UUID transform(final BsonBinary binary) {

        byte[] binaryData = binary.getData();

        switch (decoderContext.getUuidRepresentation()) {
            case C_SHARP_LEGACY:
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

        return new UUID(readLongFromArrayLittleEndian(binaryData, 0), readLongFromArrayLittleEndian(binaryData, 8));
    }

    // reverse elements in the subarray data[i:i1]
    private void reverse(final byte[] data, final int start, final int length) {
        for (int left = start, right = start+length-1; left < right; left++, right--) {
            // swap the values at the left and right indices
            byte temp = data[left];
            data[left]  = data[right];
            data[right] = temp;
        }
    }

    private static long readLongFromArrayLittleEndian(final byte[] bytes, final int offset) {
        long x = 0;
        x |= (0xFFL & bytes[offset]);
        x |= (0xFFL & bytes[offset + 1]) << 8;
        x |= (0xFFL & bytes[offset + 2]) << 16;
        x |= (0xFFL & bytes[offset + 3]) << 24;
        x |= (0xFFL & bytes[offset + 4]) << 32;
        x |= (0xFFL & bytes[offset + 5]) << 40;
        x |= (0xFFL & bytes[offset + 6]) << 48;
        x |= (0xFFL & bytes[offset + 7]) << 56;
        return x;
    }
}