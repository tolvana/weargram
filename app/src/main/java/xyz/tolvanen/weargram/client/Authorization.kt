package xyz.tolvanen.weargram.client

enum class Authorization {
    UNAUTHORIZED,
    PENDING,
    WAIT_NUMBER,
    WAIT_OTHER_DEVICE_CONFIRMATION,
    INVALID_NUMBER,
    WAIT_CODE,
    INVALID_CODE,
    WAIT_PASSWORD,
    INVALID_PASSWORD,
    AUTHORIZED,
}
