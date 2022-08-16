package xyz.tolvanen.weargram.client

enum class Authorization {
    UNAUTHORIZED,
    WAIT_NUMBER,
    INVALID_NUMBER,
    WAIT_CODE,
    INVALID_CODE,
    WAIT_PASSWORD,
    INVALID_PASSWORD,
    AUTHORIZED,
}
