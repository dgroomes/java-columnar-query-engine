package dgroomes.queryengine;

import org.opentest4j.AssertionFailedError;

public class TestUtil {

    /**
     * A convenience factory method for one of the overloaded constructors of {@link AssertionFailedError}
     */
    public static AssertionFailedError failed() {
        return new AssertionFailedError();
    }

    /**
     * A convenience factory method for one of the overloaded constructors of {@link AssertionFailedError}
     */
    public static AssertionFailedError failed(String message) {
        return new AssertionFailedError(message);
    }

    /**
     * A convenience factory method for one of the overloaded constructors of {@link AssertionFailedError}
     */
    public static AssertionFailedError failed(String message, Object expected, Object actual) {
        return new AssertionFailedError(message, expected, actual);
    }

    /**
     * A convenience factory method for one of the overloaded constructors of {@link AssertionFailedError}
     */
    public static AssertionFailedError failed(String message, Throwable cause) {
        return new AssertionFailedError(message, cause);
    }

    /**
     * A convenience factory method for one of the overloaded constructors of {@link AssertionFailedError}
     */
    public static AssertionFailedError failed(String message, Object expected, Object actual, Throwable cause) {
        return new AssertionFailedError(message, expected, actual, cause);
    }
}
