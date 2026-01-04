package depollsoft.tagmaster.lib;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RatingConverterTest {

    private RatingConverter converter;

    @Before
    public void setUp() {
        converter = new RatingConverter();
    }

    @Test
    public void convertToTarget_zeroValue_returnsZero() {
        Double source = 0.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(0, result);
    }

    @Test
    public void convertToTarget_oneValue_returns1000() {
        Double source = 1.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(1000, result);
    }

    @Test
    public void convertToTarget_halfValue_returns500() {
        Double source = 0.5;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(500, result);
    }

    @Test
    public void convertToTarget_maxFiveStarRating_returns5000() {
        Double source = 5.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(5000, result);
    }

    @Test
    public void convertToTarget_fractionalValue_truncatesCorrectly() {
        // 2.5 * 1000 = 2500
        Double source = 2.5;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(2500, result);
    }

    @Test
    public void convertToTarget_smallFractionalValue_truncatesCorrectly() {
        // 0.001 * 1000 = 1
        Double source = 0.001;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(1, result);
    }

    @Test
    public void convertToTarget_verySmallValue_returnsZero() {
        // 0.0001 * 1000 = 0.1, truncated to 0
        Double source = 0.0001;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(0, result);
    }

    @Test
    public void convertToTarget_negativeValue_returnsNegativeResult() {
        Double source = -1.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(-1000, result);
    }

    @Test
    public void convertToTarget_largeValue_returnsScaledResult() {
        Double source = 100.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(100000, result);
    }

    @Test
    public void convertToTarget_decimalPrecision_handlesCorrectly() {
        // 3.14159 * 1000 = 3141.59, truncated to 3141
        Double source = 3.14159;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(3141, result);
    }

    @Test
    public void convertToTarget_typicalRating_convertsCorrectly() {
        // A typical 4-star rating
        Double source = 4.0;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(4000, result);
    }

    @Test
    public void convertToTarget_typicalHalfStarRating_convertsCorrectly() {
        // A typical 3.5-star rating
        Double source = 3.5;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(3500, result);
    }

    @Test
    public void convertToTarget_minimalPositiveValue_convertsCorrectly() {
        // Just above 0.001 to get a result of 1
        Double source = 0.0015;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(1, result);
    }

    @Test
    public void convertToTarget_quarterValue_returns250() {
        Double source = 0.25;
        Object result = converter.convertToTarget(source, Integer.class);
        assertEquals(250, result);
    }

    @Test
    public void convertToTarget_resultIsInteger() {
        Double source = 2.7;
        Object result = converter.convertToTarget(source, Integer.class);
        assertTrue(result instanceof Integer);
    }
}
