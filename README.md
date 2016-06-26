# MakeMoji Atlas Messenger Example

An example of how to use the the Atlas Messaging Framework together with the MakeMoji Android SDK.
Refer to the original README below for Atlas instructions and [here](https://github.com/makemoji/MakemojiSDK-Android) for the MakeMoji SDK.
To run the sample, add the line "mm.key=Your-key" to local.properties or directly into the projects build.gradle so it can be loaded in App.java.


All new classes have been added under [com.layer.messenger.makemoji](app/src/main/java/com/layer/messenger/makemoji/).
Use MakeMojiAtlasComposer and MakeMojiCellFactory and to compose and display MakeMoji messages. 
Copy the new version of [atlas_message_composer.xml](app/src/main/res/layout/atlas_message_composer.xml) to your project so the composer will use a compatible MojiEditText.
Refer to the modified [MessagesListActivity](app/src/main/java/com/layer/messenger/MessagesListActivity.java) to see how to use the Atlas composer and MojiInputLayout together.

# Atlas Messenger for Android

Atlas Messenger is a fully-featured messaging app following [Material Design guidelines](https://www.google.com/design/spec/material-design/introduction.html#introduction-goals), built on top of the [Layer SDK](https://layer.com/), using the [Atlas UI toolkit](https://github.com/layerhq/Atlas-Android).

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
