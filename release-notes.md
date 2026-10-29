# FlorisBoard (fork) release

This release contains the fork specific changes of FlorisBoard:

- the actions of the smartbar can be added and removed in the settings of the app: the new screen *Settings →
  Smartbar → Quick actions* shows all the available actions, e.g. the translator, the text editor and the clipboard,
  and the sticky action of the smartbar can be selected there as well
- the translator has a button which exchanges the source language and the target language of the translation
- the source language of the translation can be selected in the translator panel and in the settings of the app
  (*Settings → Translation*), which can also detect the language of the text automatically
- the selected font is no longer lost when the backup of the theme and of the keyboard extensions is restored: the font
  which is selected in the settings is applied forcibly after every restore
- a fixed crash in the dropdowns of the settings of the app, which appeared on devices with android 8

The complete list of the changes since the first release of this fork can be found in the [changelog](CHANGELOG.md).

## Usage of the new features

The translator and the text editor are part of the quick actions of the keyboard. They can be added to the smartbar in
the settings of the app (*Settings → Smartbar → Quick actions*). The translator itself has to be enabled in the
settings of the app (*Settings → Translation*).

The translator uses the translate api of google by default, which does not need an api key. If the translate api of
yandex should be used instead, the api key has to be set in the settings of the app (*Settings → Translation*).

## Installation

Download the `florisboard-<version>-<versionCode>-release.apk` file from the assets of this release and install it
on the device. The app is a fork of FlorisBoard and is therefore signed with a self signed key, which means that the
"install from unknown sources" option has to be enabled in the settings of the device.
