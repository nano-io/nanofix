package net.nanofix.util;

import io.nano.core.clock.SystemClock;
import org.junit.Test;
import org.mockito.internal.matchers.ArrayEquals;

import static org.junit.Assert.assertThat;

/**
 * User: Mark
 * Date: 28/04/12
 * Time: 14:52
 */
public class ByteArrayConverterTest {

    private static final int TEST_LOOPS = 10 * 1000 * 1000;
    SystemClock clock = new SystemClock();

    @Test
    public void testConvertToBytes() throws Exception {
        assertThat(ByteArrayUtil.asByteArray(3), new ArrayEquals(new byte[]{'0' + 3}));
        assertThat(ByteArrayUtil.asByteArray(34), new ArrayEquals(new byte[]{'0' + 3, '0' + 4}));
        assertThat(ByteArrayUtil.asByteArray(345), new ArrayEquals(new byte[]{'0' + 3, '0' + 4, '0' + 5}));
        assertThat(ByteArrayUtil.asByteArray(3456), new ArrayEquals(new byte[]{'0' + 3, '0' + 4, '0' + 5, '0' + 6}));
    }

    @Test
    public void testConvertToBytesAsString() throws Exception {

    }

    @Test
    public void testConvertToBytesPerformance() {
        long start = clock.currentTimeMillis();
        byte[] bytes = null;
        for (int i = 0; i <= TEST_LOOPS; i++) {
            bytes = ByteArrayUtil.asByteArray(i % 10000);
        }
        printResults("convertToBytes", clock.currentTimeMillis() - start);
    }

    @Test
    public void testConvertToBytesCachedPerformance() {
        long start = clock.currentTimeMillis();
        byte[] bytes = null;
        for (int i = 0; i <= TEST_LOOPS; i++) {
            bytes = ByteArrayUtil.asByteArray(i % 10000);
        }
        printResults("convertToBytesCached", clock.currentTimeMillis() - start);
    }

    @Test
    public void testConvertToBytesAsStringPerformance() {
        long start = clock.currentTimeMillis();
        byte[] bytes = null;
        for (int i = 0; i <= TEST_LOOPS; i++) {
            bytes = ByteArrayUtil.intToBytesAsString(i % 10000);
        }
        printResults("intToBytesAsString", clock.currentTimeMillis() - start);
    }

    private void printResults(String msg, long elapsed) {
        System.out.println(TEST_LOOPS + " x " + msg + " took " + elapsed + "ms @ "
                + (double) elapsed * 1000 * 1000 / (double) TEST_LOOPS + " ns");
    }

}
