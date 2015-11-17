# Atlas Messenger for Android

Atlas Messenger is a fully-featured messaging app following [Material Design guidelines](https://www.google.com/design/spec/material-design/introduction.html#introduction-goals), built on top of the [Layer SDK](https://layer.com/), using the [Atlas UI toolkit](https://github.com/layerhq/Atlas-Android).

## Structure

* **App:** Application class.
* Activities:
  * **BaseActivity:** Base Activity class for handling menu titles and the menu back button and ensuring the `LayerClient` is connected when resuming Activities.
  * **ConversationsListActivity:** List of all Conversations for the authenticated user.
  * **MessagesListActivity:** List of Messages within a particular Conversation.  Also handles message composition and addressing.
  * **SettingsActivity:** Global application settings.
  * **ConversationDetailsActivity:** Settings for a particular Conversation.
* **PushNotificationReceiver:** Handles `com.layer.sdk.PUSH` Intents and displays notifications.
* **AuthenticationProvider:** Interface used by the Messenger app to authenticate users.  Implementations are provided in two flavors; see *Build Variants* below.

## Build Variants

Atlas Messenger uses different [product flavors](http://developer.android.com/tools/building/configuring-gradle.html#workBuildVariants) for authenticating with different backends ("Identity Providers").  Each flavor implements a custom `AuthenticationProvider` and Atlas `Participant`, and provides login Activities specific to their required credentials.  Two such flavors are provided:

1. **atlasprovider:** For use in the [Layer Atlas demo](https://getatlas.layer.com/android).
2. **herokuprovider:** For use with the deployable [Rails Provider](https://github.com/layerhq/layer-identity-provider) backend.

In Android Studio, switch flavors using Build Variants, typically in the side tab on the lower left of the Android Studio window.

## Notifications

`PushNotificationReceiver.Notifications` manages notifications, displaying notifications grouped by `Conversation` using [InboxStyle](http://developer.android.com/reference/android/support/v4/app/NotificationCompat.InboxStyle.html).  When expanded, the last five notified messages are displayed.  `MessagesListActivity` automatically clears associated notifications when launching.

## Setup

`git submodule init`
`git submodule update`
