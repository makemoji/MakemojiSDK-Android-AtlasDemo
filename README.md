# Atlas Messenger for Android

Atlas Messenger is a fully-featured messaging app following [Material Design guidelines](https://www.google.com/design/spec/material-design/introduction.html#introduction-goals), built on top of the [Layer SDK](https://layer.com/), using the [Atlas UI toolkit](https://github.com/layerhq/Atlas-Android).

##<a name="setup"></a>Setup

Run `git submodule update --init` to initialize and pull in the [Atlas-Android](https://github.com/layerhq/Atlas-Android) submodule.

##<a name="structure"></a>Structure

* **App:** Application class.
* Activities:
  * **BaseActivity:** Base Activity class for handling menu titles and the menu back button and ensuring the `LayerClient` is connected when resuming Activities.
  * **ConversationsListActivity:** List of all Conversations for the authenticated user.
  * **MessagesListActivity:** List of Messages within a particular Conversation.  Also handles message composition and addressing.
  * **SettingsActivity:** Global application settings.
  * **ConversationDetailsActivity:** Settings for a particular Conversation.
* **PushNotificationReceiver:** Handles `com.layer.sdk.PUSH` Intents and displays notifications.
* **AuthenticationProvider:** Interface used by the Messenger app to authenticate users.  Default implementations are provided by gradle `flavors`; see *Build Variants* below.

##<a name="identityproviders"></a>Identity Providers

Atlas Messenger uses the `AuthenticationProvider` interface to authenticate with various backends.  Additional identity providers can integrate with Atlas Messenger by implementing `AuthenticationProvider` and using a custom login Activity, similar to the provided flavors below.

###<a name="buildvariants"></a>Provided Flavors
Two default implementations are provided via [product flavors](http://developer.android.com/tools/building/configuring-gradle.html#workBuildVariants), where each flavor implements a custom `AuthenticationProvider`, a custom Atlas `Participant`, and provides login Activities for gathering their required credentials:  

1. **atlasprovider:** For use in the [Layer Atlas demo](https://getatlas.layer.com/android).  This authentication flow utilizes a QR-Code scanner to capture a Layer App ID from the Layer developer dashboard.  The scanner can be bypassed by supplying your Atlas demo App ID in the `App.LAYER_APP_ID` constant. 
2. **herokuprovider:** For use with the deployable [Rails Provider](https://github.com/layerhq/layer-identity-provider) backend.  Your Layer App ID must be set in the `App.LAYER_APP_ID` constant.

In Android Studio, switch flavors using Build Variants, typically in the side tab on the lower left of the Android Studio window.

##<a name="contributing"></a>Contributing
Atlas is an Open Source project maintained by Layer. Feedback and contributions are always welcome and the maintainers try to process patches as quickly as possible. Feel free to open up a Pull Request or Issue on Github.

##<a name="license"></a>License

Atlas is licensed under the terms of the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please see the [LICENSE](LICENSE) file for full details.

##<a name="contact"></a>Contact

Atlas was developed in San Francisco by the Layer team. If you have any technical questions or concerns about this project feel free to reach out to [Layer Support](mailto:support@layer.com).

###<a name="credits"></a>Credits

* [Steven Jones](https://github.com/sjones94549)
