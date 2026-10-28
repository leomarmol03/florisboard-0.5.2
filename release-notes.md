# FlorisBoard (fork) release

This release contains the fork specific changes of FlorisBoard:

- the clipboard items can be pinned, unpinned, edited and shared again: a long click on an item opens the actions of
  the item, while the multi selection is still available with the button in the header of the clipboard
- an integrated translator, which uses the translate api of yandex or google and translates the text of the input
  field, like the translator of gboard
- a text editor, which is shown above the keys of the keyboard and which can be used to write a text or to edit an
  item of the clipboard
- the selected font is no longer lost when a backup is restored: the font which is selected in the settings is applied
  forcibly after the restore and the file of the font is kept
- word suggestions for english and spanish with a hint of the next word
- the languages of the suggestions can be selected in the settings
- an automatic correction which fixes the typed words, if a similar word exists in the word list of the active language
- a custom font which can be added from the storage of the device (TTF/OTF) and is used in the keyboard, the clipboard
  and the settings
- clipboard tabs for the pinned and the recent items, swipe to delete and multi selection with delete and select all
- adding a language and its layout fills the remaining fields automatically and activates the new language
- material you dynamic colors for the xml based resources
- a fixed emoji palette which no longer lags
- a signed release build which is published by the continuous integration

## Usage of the new features

The actions of the translator and of the text editor are part of the quick actions of the keyboard. They can be added
to the smartbar in the settings of the app (*Settings → Smartbar → Customize actions*).

The translator uses the translate api of google by default, which does not need an api key. If the translate api of
yandex should be used instead, the api key has to be set in the settings of the app (*Settings → Translation*).

## Installation

Download the `florisboard-<version>-<versionCode>-release.apk` file from the assets of this release and install it
on the device. The app is a fork of FlorisBoard and is therefore signed with a self signed key, which means that the
"install from unknown sources" option has to be enabled in the settings of the device.
