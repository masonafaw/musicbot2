/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.db;

import fredboat.db.rest.BackendException;
import fredboat.util.func.NonnullSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Conversion of exceptions from communicating with the backend to userfriendly ones
 */
public class FriendlyEntityService {

    private static final Logger log = LoggerFactory.getLogger(FriendlyEntityService.class);

    //this is a utility class
    private FriendlyEntityService() {
    }

    /**
     * Wrap an operation that throws a database exception so that it gets rethrown as one of our user friendly
     * MessagingExceptions. MessagingExceptions or their causes are currently not expected to be logged further up,
     * that's why we log the cause of it at this place.
     */
    public static <T> T fetchUserFriendly(NonnullSupplier<T> operation) {
        try {
            return operation.get();
        } catch (BackendException e) {
            log.error("EntityService database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }

    /**
     * Same as {@link FriendlyEntityService#fetchUserFriendly(NonnullSupplier)}, just with a nullable return.
     */
    @Nullable
    public static <T> T getUserFriendly(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (BackendException e) {
            log.error("EntityService database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }

    /**
     * Same as {@link FriendlyEntityService#fetchUserFriendly(NonnullSupplier)}, just without returning anything
     */
    public static void doUserFriendly(Runnable operation) {
        try {
            operation.run();
        } catch (BackendException e) {
            log.error("EntityService database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }
}
