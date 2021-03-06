package net.nanofix.util;

public final class FIXBytes {

    public static final byte SOH = (byte) 0x01;
    public static final byte EQUALS = '=';
    public static final byte PIPE = '|';
    public static final byte FIX_TRUE = 'Y';
    public static final byte FIX_FALSE = 'N';
    public static final byte BODY_LEN_TAG = '9';

    public static final byte[] BEGIN_STRING_PREFIX = ByteArrayUtil.asByteArray("8=FIX.");
    public static final byte[] CHECKSUM_PREFIX = ByteArrayUtil.asByteArray("10=");
    public static final byte[] CHECKSUM_PLACEHOLDER = ByteArrayUtil.asByteArray("000");
    public static final byte[] MSG_TYPE_TAG_BYTES = ByteArrayUtil.asByteArray("35");

    private FIXBytes() {
        // can't touch this
    }

}
