# Weargram

Telegram client for Wear OS based on tdlib and Compose.

Note that this is by no means a fully functioning Telegram client,
as many features are still either missing or unstable.

Fully or partially implemented features include:
- Login (with either a phone number + authentication code, or QR code)
- View private and group chats
- View user and group information
- Send text messages
- View image, video, animation, location, and sticker messages
- Listen to audio messages
- Notifications

Missing (for now) features include:
- Send audio, sticker, and location messages
- Reactions

## Building

Building should work on at least Android Studio 2022.1.1 Canary 9.

Remember to replace the Telegram API access details in `app/src/main/res/values/api.xml`
if you intend to use the app for more than just testing or development.

## Known issues
- Chats are sometimes fetched incorrectly or with missing information in the main view
- Occasional poor performance
