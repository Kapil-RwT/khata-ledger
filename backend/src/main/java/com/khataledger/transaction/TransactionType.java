package com.khataledger.transaction;

/**
 * CREDIT  = merchant gave goods on udhaar  -> customer's outstanding goes UP
 * DEBIT   = merchant received cash back    -> customer's outstanding goes DOWN
 */
public enum TransactionType {
    CREDIT,
    DEBIT
}
