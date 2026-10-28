# FlorisBoard (fork) release

This release contains the fork specific changes of FlorisBoard:

- word suggestions for english and spanish with a hint of the next word
- the languages of the suggestions can be selected in the settings
- an automatic correction which fixes the typed words, if a similar word exists in the word list of the active language
- a custom font which can be added from the storage of the device (TTF/OTF) and is used in the keyboard, the clipboard and the settings
- clipboard tabs for the pinned and the recent items, swipe to delete and multi selection with delete and select all
- adding a language and its layout fills the remaining fields automatically and activates the new language
- material you dynamic colors for the xml based resources
- a fixed emoji palette which no longer lags
- a signed release build which is published by the continuous integration

## Installation

Download the `florisboard-<version>-<versionCode>-release.apk` file from the assets of this release and install it
on the device. The app is a fork of FlorisBoard and is therefore signed with a self signed key, which means that the
"install from unknown sources" option has to be enabled in the settings of the device.

The keyboard can be enabled in the settings of the device under "System > Languages & input > FlorisBoard".
