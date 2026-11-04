# Security Specification — My Saving Tracker

## 1. Data Invariants
1. **Strict Multi-Tenant Isolation**: Every document in `/users/{userId}/saving_plans/{planId}` and `/users/{userId}/payments/{dateIso}` belongs exclusively to `userId` and may only be read, created, updated, or deleted when `request.auth != null && request.auth.uid == userId`.
2. **Duplicate Prevention via Deterministic Keys**: Each saving payment record in `/users/{userId}/payments/{dateIso}` must have a `dateIso` key matching `^202[67]-[01][0-9]-[0-3][0-9]$` and `incoming().dateIso == dateIso`.
3. **Financial Conservation Invariant**:
   - `totalTarget` must strictly equal `10400` (`₹10,400`).
   - `amount` on every payment record must strictly equal `100` (`₹100`).
   - On `SavingPlanSummary`, `totalSavedAmount + remainingAmount == 10400` and `paidDates.size() <= 104`.
4. **Temporal & Immutable Identity**:
   - `createdAt` and `updatedAt` must be valid Firestore server timestamps (`<= request.time`).
   - On `update`, `userId`, `dateIso`, and `createdAt` are strictly immutable.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get` or `set` on `/users/alice_123/payments/2026-12-02`.
2. **Cross-User Read (Horizontal Privilege Escalation)**: `bob_456` reading `/users/alice_123/payments/2026-12-02`.
3. **Cross-User Write (Identity Spoofing)**: `bob_456` creating `/users/alice_123/payments/2026-12-02` or setting `userId: "alice_123"`.
4. **Shadow Field Injection (Ghost Key)**: Adding `"isAdmin": true` or `"bonus": 5000` to `SavingPaymentRecord` on create or update.
5. **Invalid Payment Status Enum**: Setting `status: "FREE"` instead of `"PAID"` or `"DUE"`.
6. **Tampered Saving Amount**: Setting `amount: 500` or `amount: -100` instead of `100`.
7. **Tampered Total Target**: Setting `totalTarget: 999999` instead of `10400` or violating `totalSavedAmount + remainingAmount == 10400`.
8. **Oversized Transaction Note (Denial of Wallet)**: Setting `transactionNote` to a 5,000-character string (`> 300`).
9. **Path vs Body Date Mismatch**: Writing `dateIso: "2026-12-04"` to document `/users/alice_123/payments/2026-12-02`.
10. **Immutable Field Mutation on Update**: Attempting to mutate `createdAt` or `userId` during an `update` call.
11. **Future Timestamp Spoofing**: Submitting `createdAt` or `updatedAt` in the future (`> request.time`) or as an integer epoch instead of a `timestamp`.
12. **Unbounded Array Poisoning**: Submitting `paidDates` with more than 104 items or non-string element types.
