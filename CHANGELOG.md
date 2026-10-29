# Changelog

All notable changes of this fork of [FlorisBoard](https://github.com/florisboard/florisboard) are documented in this
file. The versions are listed from the newest to the oldest one. The versions before `v0.5.2` are the versions of the
upstream project and are documented in the
[changelog of FlorisBoard](https://github.com/florisboard/florisboard/blob/develop/CHANGELOG.md).

## v0.7.1 (version code 120)

### Added

- a new screen in the settings of the app (*Settings → Smartbar → Quick actions*), in which the actions of the
  smartbar can be added and removed: the translator, the text editor, the clipboard and many other actions are
  available there. The sticky action of the smartbar can be selected there as well
- a button in the translator panel, which exchanges the source language and the target language of the translation
- a selection of the source language of the translation in the translator panel and in the settings of the app
  (*Settings → Translation*), which can also detect the language of the text automatically

### Fixed

- the selected font is no longer lost when the backup of the theme and of the keyboard extensions is restored: the font
  which is selected in the settings is applied forcibly after every restore
- a crash in the dropdowns of the settings of the app (the languages and layouts of the keyboard, the properties and
  the rules of the theme), which appeared on devices with android 8

## v0.6.0 (version code 119)

### Added

- an integrated translator, which uses the translate api of google or yandex and translates the text of the input
  field, like the translator of gboard
- a text editor, which is shown above the keys of the keyboard and which can be used to write a text or to edit an
  item of the clipboard, like the text editor of gboard
- the items of the clipboard can be pinned, unpinned, edited and shared again: a long click on an item opens the
  actions of the item, while the multi selection is still available with the button in the header of the clipboard
- word suggestions for english and spanish with a hint of the next word
- an automatic correction which fixes the typed words, if a similar word exists in the word list of the active language

### Fixed

- the selected font is no longer lost when a backup is restored: the font which is selected in the settings is applied
  forcibly after the restore and the file of the font is kept

## v0.5.7 (version code 118)

### Added

- adding a language and its layout fills the remaining fields of the form automatically and activates the new
  language right away
- an automatic correction, which can be switched off in the settings of the app
- a selection of the languages for which the suggestions should be shown in the settings of the app

### Fixed

- a restored backup no longer loses the font which was imported by the user
- only one build workflow is used from now on, the workflow of the stable releases

## v0.5.2 (version code 117)

This is the first release of this fork of FlorisBoard.

### Added

- hints in the keyboard, which show the text which was typed last and the texts are available in english and in
  spanish
- a swipe to one side deletes an item of the clipboard
- tabs in the clipboard, which separate the pinned items from the recent ones. The last active tab is remembered
- material you dynamic colors for the xml based resources of the app
- an import of a custom font (TTF/OTF) from the storage of the device, which is used in the keyboard, in the
  clipboard and in the settings of the app. The bundled font was removed with this release
- a multi selection of the items of the clipboard with delete and select all
- an automatic compilation of the release build, which is signed and published as a release of the repository

### Fixed

- the suggestions are dismissed in a smoother way while typing
- the backups can be restored correctly, also with the fonts and the extensions of the keyboard
