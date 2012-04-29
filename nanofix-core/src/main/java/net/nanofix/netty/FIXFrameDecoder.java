package net.nanofix.netty;

import net.nanofix.message.FIXConstants;
import net.nanofix.message.MessageException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * User: Mark Wardell
 * Date: 10/10/11
 * Time: 14:49
 */
public class FIXFrameDecoder extends FrameDecoder {

    private final static Logger LOG = LoggerFactory.getLogger(FIXFrameDecoder.class);

    private static final byte[] beginStringByteArray = new byte[FIXConstants.BEGIN_STRING_PREFIX_FIXT.length()];

    public static final int DEFAULT_MAX_FRAME_LENGTH = 9999;
    public static final int MIN_LENGTH_TAG_OFFSET = 12;
    public static final int MAX_LENGTH_BYTES = 5;
    public static final int LENGTH_OF_TRAILER_BYTES = 7;

    private static final Charset charset = Charset.forName("US-ASCII");
    public static final int NOT_FOUND = -1;
    private final int maxFrameLength;

    public FIXFrameDecoder() {
        this.maxFrameLength = DEFAULT_MAX_FRAME_LENGTH;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

        try {
            // Make sure if the bodyLength field was received.
            if (buffer.readableBytes() <= MIN_LENGTH_TAG_OFFSET + 1) {
                // The bodyLength field was not received yet - return null.
                // This method will be invoked again when more packets are
                // received and appended to the buffer.
                return null;
            }

            validateBeginString(buffer);

            final int messageLength = getMessageLength(buffer);
//            logger.trace("readableBytes={} BodyLength={} TotalLength={} Buffer[{}]",
//                    new Object[] { buffer.readableBytes(), lengthStr, totalLength, toString(buffer.slice()) });

            if (messageLength > maxFrameLength) {
                throw new TooLongFrameException("Frame bodyLength may not be greater than " + maxFrameLength + " bytes");
            }

            // There are enough bytes in the buffer. Read it.
            if (buffer.readableBytes() >= messageLength) {
                ChannelBuffer newFrame = buffer.readBytes(messageLength);

                // make sure it ends well
                validateChecksum(newFrame.array());

                // pass this up the pipeline
                return newFrame;
            }
        }
        catch (Exception e) {
            LOG.warn("Error in decoder: " + e.toString());
            LOG.info("Above error caused by message: " + buffer.toString(Charset.defaultCharset()));
            throw e;
        }
        return null;
    }

    /**
     * validates message to make sure that it starts with begin string (i.e. 8=FIX. or 8=FIXT.)
     *
     * @param buffer the given ChannelBuffer containing a possible FIX message that requires validation
     * @throws CorruptedFrameException if the buffer does not start with the required FIX identifier
     */
    private void validateBeginString(ChannelBuffer buffer) throws CorruptedFrameException {

        // clear the array first
        Arrays.fill(beginStringByteArray, (byte) 0);

        // test for normal FIX first
        buffer.getBytes(buffer.readerIndex(), beginStringByteArray, 0, FIXConstants.BEGIN_STRING_PREFIX.length());
        if (Arrays.equals((FIXConstants.BEGIN_STRING_PREFIX + "\u0000").getBytes(), beginStringByteArray))
            return;

        LOG.debug("expected=[{}] actual=[{}]",
            FIXConstants.BEGIN_STRING_PREFIX + "\u0000",
            new String(beginStringByteArray)
        );

        // now test for FIXT
        buffer.getBytes(buffer.readerIndex(), beginStringByteArray, 0, FIXConstants.BEGIN_STRING_PREFIX_FIXT.length());
        if (Arrays.equals(FIXConstants.BEGIN_STRING_PREFIX_FIXT.getBytes(), beginStringByteArray))
            return;
        System.out.println();
        throw new CorruptedFrameException("FIX message should begin with \"8=FIX.\" or \"8=FIXT.\"");
    }

    private int getMessageLength(ChannelBuffer buffer) throws CorruptedFrameException, TooLongFrameException {

        // find the delimiter after the first tag + 3 (skip delimiter + '9=')
        final int startOfLength = buffer.bytesBefore(FIXConstants.SOH) + 3;
        if (startOfLength == NOT_FOUND)
            throw new CorruptedFrameException("BodyLength(9) should be the second field in the message");

        final int lengthOfLength = buffer.bytesBefore(buffer.readerIndex() + startOfLength,
                MAX_LENGTH_BYTES, FIXConstants.SOH);
        if (lengthOfLength == NOT_FOUND) {
            if (buffer.readableBytes() > MAX_LENGTH_BYTES) {
                // too many characters read, but no BodyLength field found
                throw new TooLongFrameException("End of BodyLength field not found within "
                        + (MAX_LENGTH_BYTES + MIN_LENGTH_TAG_OFFSET) + " bytes");
            }

            // end of BodyLength field not found
            return NOT_FOUND;
        }

        final String lengthStr = toString(buffer.slice(buffer.readerIndex() + startOfLength, lengthOfLength));
        final int bodyLength = Integer.parseInt(lengthStr);
        return (startOfLength + lengthOfLength + 1) + bodyLength + LENGTH_OF_TRAILER_BYTES;
    }

    private String toString(ChannelBuffer buf) {
        return buf.toString(charset);
    }

    /**
     * Confirm that end of message has the checksum - i.e. 10=xxx<SOH>
     * and that the calculated checksum is correct
     *
     * @param bytes
     * @throws net.nanofix.message.MessageException
     */
    protected static void validateChecksum(byte[] bytes) throws MessageException {

        // make sure checksum is there
        validateChecksumPresent(bytes);

        // confirm that end of message has the checksum - i.e. 10=xxx<SOH>
        int computedChecksum = calcChecksumAsInt(bytes);
        int receivedChecksum = 0;
        try {
            receivedChecksum = Integer.parseInt(new String(bytes, bytes.length - 4, 3));
        } catch (NumberFormatException e) {
            throw new MessageException("Checksum failure");
        }

        if (receivedChecksum != computedChecksum)
            throw new MessageException("Checksum failure");
    }

    private static void validateChecksumPresent(byte[] bytes) throws MessageException {
        if (!Arrays.equals(FIXConstants.CHECKSUM_PREFIX.getBytes(),
                Arrays.copyOfRange(bytes, bytes.length-7, bytes.length-4))) {
            throw new MessageException("Missing checksum field [" + FIXConstants.CHECKSUM_PREFIX
                    + "] at end of message");

        }
    }

    protected static int calcChecksumAsInt(byte[] bytes) throws MessageException {
        int checksum = 0;
        for (int i = 0; i < bytes.length - FIXConstants.CHECKSUM_SIZE - 1; i++) {
            checksum += (char) bytes[i];    // cast to unsigned char
        }
        return (checksum + 1) % 256;
    }

    /**
     * This method is a crude attempt to extract 34=NNN from an invalid FIX messages that cannot be parsed
     * using the normal parsing technique.
     * @param bytes the invalid FIX message
     * @return the SeqNum extracted from the string
     */
    private long extractSeqNum(byte[] bytes) {
        long seqNum = -1L;
        String msg = new String(bytes);
        int index = msg.indexOf(FIXConstants.SOH + "34=");
        if (index == -1) {
            // not found
            return seqNum;
        }
        int endIndex = msg.indexOf(FIXConstants.SOH, index + 3);
        if (endIndex == -1) {
            // not found
            return seqNum;
        }
        try {
            seqNum = Long.parseLong(msg.substring(index + 3, endIndex));
        }
        catch (NumberFormatException e) {
            // ignore, best efforts here
        }
        return seqNum;
    }


}
