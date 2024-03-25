package edu.berkeley.cs186.database.concurrency;

import edu.berkeley.cs186.database.TransactionContext;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock
 * acquisition for the user (you, in the last task of Part 2). Generally
 * speaking, you should use LockUtil for lock acquisition instead of calling
 * LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that the current transaction can perform actions requiring
     * `requestType` on `lockContext`.
     *
     * `requestType` is guaranteed to be one of: S, X, NL.
     *
     * This method should promote/escalate/acquire as needed, but should only
     * grant the least permissive set of locks needed. We recommend that you
     * think about what to do in each of the following cases:
     * - The current lock type can effectively substitute the requested type
     * - The current lock type is IX and the requested lock is S
     * - The current lock type is an intent lock
     * - None of the above: In this case, consider what values the explicit
     *   lock type can be, and think about how ancestor looks will need to be
     *   acquired or changed.
     *
     * You may find it useful to create a helper method that ensures you have
     * the appropriate locks on all ancestors.
     */
    public static void ensureSufficientLockHeld(LockContext lockContext, LockType requestType) {
        // requestType must be S, X, or NL
        assert (requestType == LockType.S || requestType == LockType.X || requestType == LockType.NL);

        // Do nothing if the transaction or lockContext is null
        TransactionContext transaction = TransactionContext.getTransaction();
        if (transaction == null || lockContext == null) return;

        // You may find these variables useful
        LockContext parentContext = lockContext.parentContext();
        LockType effectiveLockType = lockContext.getEffectiveLockType(transaction);
        LockType explicitLockType = lockContext.getExplicitLockType(transaction);

        // TODO(proj4_part2): implement
        //The current lock type can effectively substitute the requested type
        if (LockType.substitutable(effectiveLockType, requestType) ||
                LockType.substitutable(explicitLockType, requestType)) {
            return;
        }

        if (parentContext != null) {
            //make sure all ancestors have correct locks
            ensureAppropriateLocks(parentContext, LockType.parentLock(requestType), transaction);
        }

        //The current lock type is IX and the requested lock is S
        if (explicitLockType == LockType.IX && requestType == LockType.S) {
            //promote to SIX lock to combine IX and S locks
            lockContext.promote(transaction, LockType.SIX);
            return;
        }

        //The current lock type is an intent lock
        if (explicitLockType.isIntent()) {
            //move up children
            lockContext.escalate(transaction);
            return;
        }

        //None of the above: In this case, consider what values the explicit
        //lock type can be, and think about how ancestor looks will need to be
        // acquired or changed.

        //Can only be NL, S, X
        if (explicitLockType == LockType.S || explicitLockType == LockType.X) {
            lockContext.promote(transaction, requestType);
        }
        else { // no lock
            lockContext.acquire(transaction, requestType);
        }

        return;
    }

    // TODO(proj4_part2) add any helper methods you want

    //helper method that ensures you have the appropriate locks on all ancestors.
    private static void ensureAppropriateLocks(LockContext LC, LockType requestType, TransactionContext transaction) {
        //if root, return
        if (LC == null) {
            return;
        }

        LockType effectiveLockType = LC.getEffectiveLockType(transaction);
        LockType explicitLockType = LC.getExplicitLockType(transaction);

        //works correctly
        if (LockType.substitutable(effectiveLockType, requestType) ||
                LockType.substitutable(explicitLockType, requestType)) {
            return;
        }

        LockContext parentContext = LC.parentContext();

        //recurse through tree, fix top down
        ensureAppropriateLocks(parentContext, LockType.parentLock(requestType), transaction);

        if (explicitLockType.isIntent()) { //intent lock?
            LC.promote(transaction, requestType);
        } else { //no lock?
            LC.acquire(transaction, requestType);
        }

        return;

    }

}
