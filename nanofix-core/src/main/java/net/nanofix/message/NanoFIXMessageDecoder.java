package net.nanofix.message;

import net.nanofix.message.util.ChecksumCalculator;
import net.nanofix.util.FIXBytes;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static net.nanofix.message.ByteBufferUtil.NOT_FOUND_INDEX;
import static net.nanofix.util.FIXBytes.EQUALS;
import static net.nanofix.util.FIXBytes.SOH;

/**
 * The first three fields are mandatory and must appear in this order:
 *
 * BeginString (8) - Identifies the beginning of a FIX message. E.g. 8=FIX.4.4.
 *
 * BodyLength (9) - The number of bytes in the message following the BodyLength (9) field up to, and including,
 * the delimiter immediately preceding the CheckSum (10) field.
 *
 * MsgType (35) - Defines message type. E.g. 35=A.
 *
 * CheckSum (10) - Always the last field and the value always contains 3 bytes. E.g. 10=093.
 * Calculated as modulo 256 of the sum of every byte in the message up to but not including the checksum field itself.
 *
 * User: Mark Wardell
 * Date: 10/10/16
 * Time: 14:34
 */
public class NanoFIXMessageDecoder implements FIXMessageDecoder {

    private static final String EQUAL_NOT_FOUND_ERROR_MESSAGE = "Tag value delimiter '=' not found after index.";
    private static final String SOH_NOT_FOUND_ERROR_MESSAGE = "Field delimiter 'SOH' not found after index.";
    private static final String BEGIN_STRING_ERROR_MESSAGE = "Message must start with with the correct begin string 8=FIX.";
    private static final String BODY_LEN_SECOND_FIELD_ERROR_MESSAGE = "BodyLength(9) should be the second field in the message";
    private static final String BODY_LEN_INCORRECT_ERROR_MESSAGE = "BodyLength(9) value is incorrect";
    private static final String BODY_LEN_INVALID_ERROR_MESSAGE = "BodyLength(9) value is invalid.";
    private static final String CHECKSUM_INCORRECT_ERROR_MESSAGE = "Invalid checksum!";

    private static final int MIN_BODY_LEN = 5; // 8=FIX.4.x|9=NN|35=X|10=nnn|
    private static final int MAX_BODY_LEN = 1024 * 1024;

    @Override
    public void decode(ByteBuffer buffer, FIXMessageVisitor visitor) {
        // start at current position?
        int initialOffset = 0;

        // initialise a few counters
        int bodyLen = 0;
        int bodyStartIndex = 0;
        int tagIndex = initialOffset;
        int tagCount = 0;
        while (tagIndex < buffer.remaining()) {
            int equalIndex = ByteBufferUtil.indexOf(buffer, tagIndex, EQUALS);
            if (equalIndex == NOT_FOUND_INDEX) {
                visitor.onError(tagIndex, EQUAL_NOT_FOUND_ERROR_MESSAGE);
                break;
            }
            int tagLen = equalIndex - tagIndex;
            int valueIndex = equalIndex + 1;

            int startOfHeaderIndex = ByteBufferUtil.indexOf(buffer, valueIndex, SOH);
            if (startOfHeaderIndex == NOT_FOUND_INDEX) {
                visitor.onError(valueIndex, SOH_NOT_FOUND_ERROR_MESSAGE);
                break;
            }
            int valueLen = startOfHeaderIndex - valueIndex;

            // check first tag is the FIX BeginString
            if (tagCount == 0) {
                if (!ByteBufferUtil.hasBytes(buffer, tagIndex, FIXBytes.BEGIN_STRING_PREFIX)) {
                    visitor.onError(tagIndex, BEGIN_STRING_ERROR_MESSAGE);
                    break;
                }
            }
            // check MsgBody
            else if (tagCount == 1) {
                if (!ByteBufferUtil.hasByte(buffer, tagIndex, FIXBytes.BODY_LEN_TAG_BYTE)) {
                    visitor.onError(tagIndex, BODY_LEN_SECOND_FIELD_ERROR_MESSAGE);
                    break;
                }
                if (valueLen > 4) {
                    visitor.onError(tagIndex, BODY_LEN_INVALID_ERROR_MESSAGE);
                }
                bodyLen = ByteBufferUtil.toInt(buffer, valueIndex, valueLen);

                if (bodyLen < MIN_BODY_LEN || bodyLen > MAX_BODY_LEN) {
                    visitor.onError(tagIndex, BODY_LEN_INVALID_ERROR_MESSAGE);
                    break;
                }

                // return and wait for more data
                if (bodyLen > buffer.limit()) {
                    break;
                }
            }

            // last consistency check for checksum field
            if (ByteBufferUtil.hasBytes(buffer, tagIndex, FIXBytes.CHECKSUM_PREFIX)) {
                int actualBodyLength = tagIndex - bodyStartIndex;
                if (bodyLen != actualBodyLength) {
                    visitor.onError(tagIndex, BODY_LEN_INCORRECT_ERROR_MESSAGE);
                    break;
                }
                // check that checksum value is correct
                int checksum = ByteBufferUtil.toInt(buffer, valueIndex, valueLen);
                int calculatedChecksum = ChecksumCalculator.calculateChecksum(
                        buffer, initialOffset, tagIndex - initialOffset);

                if (checksum != calculatedChecksum) {
                    System.out.println("checksum:" + checksum + " calculatedChecksum: " + calculatedChecksum+
                    " real one: " +
                    checksum("8=FIX.4.3|9=72|35=0|49=CLIENT|56=BROKER|34=42|52=19700101-00:00:00.000|112=test-req-id|".replaceAll("\\|", String.valueOf((char)FIXBytes.SOH))));
                    visitor.onError(tagIndex, CHECKSUM_INCORRECT_ERROR_MESSAGE);
                    // TODO break;
                }
            }

            // notify visitor of next tag value pair
            visitor.onTag(buffer, tagIndex, tagLen, valueLen);

            // move offset to next available byte
            tagIndex += (tagLen + valueLen + 2);

            // keep a record of when the body starts
            if (tagCount == 1) {
                bodyStartIndex = tagIndex;
            }

            // increment the tag counter
            tagCount++;
        }
    }

    static int checksum(String text) {
        return ChecksumCalculator.calculateChecksum(
                ByteBuffer.wrap(text.getBytes(StandardCharsets.US_ASCII)),
                0, text.length()
        );
    }
}