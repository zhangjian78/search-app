package amz_iap

enum class IapEnableStatus {
    On, //IAP logic activated
    DisabledAndDefaultAsFree, //IAP logic disabled, with "paid" feature disabled.
    DisabledAndDefaultAsPaid, //IAP logic disabled, with "paid" feature enabled.
}