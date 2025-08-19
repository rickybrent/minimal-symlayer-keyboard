# Minimal SymLayer Keyboard

`Minimal SymLayer Keyboard` is an Android input method for the Minimal Phone MP01, though it may be useful for other devices with built in thumbboards. It does not contain a virtual keyboard but adds a SYM layer for physical keys.

The project is a fork of the excellent  [TitanPocketKeyboard by oin](https://github.com/oin/titanpocketkeyboard), which was originally designed for the Unihertz Titan Pocket, and has no relation to Lersi's [Minimal Phone Keyboard](https://github.com/lersi/minimal_phone_keyboard) or the keyboard that shipped with the Minimal Phone.

This fork adds several new features (a keyboard-focused emoji picker, a clipboard history manager, and additional virtual modifier keys) and special handling for the MP01's unique keys and e-ink display.

### A Note for Unihertz Titan Pocket Users

While this project is based on the TitanPocketKeyboard, support for the Unihertz Titan Pocket is **entirely untested** in this fork. Many of the improvements are specific to the Minimal Phone's hardware. However, features like the keyboard-navigable emoji picker and clipboard history might be of interest to Titan Pocket users willing to experiment, and the code has been written with a blind best-effort to support both devices.


## Key Features of this Fork

This fork builds upon the original's solid foundation with several new features and improvements, primarily for the Minimal Phone MP01:

* **Keyboard-Focused Emoji Picker**: A searchable emoji filter accessed by tapping the Emoji/0 key.
* **Clipboard History**: A clipboard manager with search and pinning, accessed via Emoji/0 + V.
* **Sym-Layer Preview**: A visual preview of the Sym layer mappings, making it easy to discover and learn.
* **Advanced Key Support for MP01**: Added support for the Minimal Phone's key layout, adding additional functionality over the factory keyboard:
    * **Three-Way Modifier Keys**: The Mic/Period and Emoji/0 keys act as multi-function modifiers:
        * **Mic/Period Key**:
            * **Tap**: Inserts a period (.), or > when Shift is held.
            * **Hold + Key**: Acts as a Ctrl modifier for keyboard shortcuts (e.g., Hold Mic/Period + C for Copy).
            * **Long Press & Release**: Invokes the voice-to-text input (also triggered by Alt + Mic/Period).
        * **Emoji/0 Key**:
            * **Tap**: Opens the emoji picker.
            * **Hold + Key**: Acts as a special function modifier (like the Meta/Win/Command key). For example, Emoji + V opens the clipboard history viewer.
            * **Long Press & Release**: Inserts a 0 (also triggered by pressing while holding Alt).
* **Sym layer updated for the MP01**: The keys on the sym layer have been updated and changed to avoid duplicating any of the alt-keys already available, and to make it possible to enter certain common keys missing from the MP01's alt layer (such as parenthesis.)

The Multipress for accented characters feature from the original project is disabled by default, but is still included.


## Installation

1. Download the latest APK from the [releases](https://github.com/rickybrent/minimal-symlayer-keyboard/releases) page. (Not yet available)
2. Install the APK on your Minimal Phone MP01.
3. Go to `Settings` > `System` > `Languages & input` > `Virtual keyboard` > `Manage keyboards` and enable `Minimal Symlayer Input`.
4. Select `Minimal Symlayer Input` as your default input method.




## Core Features from original TitanPocketKeyboard

* **Full Keyboard Layout** with all keys and symbols.
* No space taken on the screen.
* Long press for alternate characters (see list below).
* **Keyboard Navigation**: simulate arrow keys and home/end/page up/page down keys, using the `sym` modifier - especially useful with an e-ink screen.
* Lock modifier keys by double-tapping them, while a single tap will only have effect for the next key press.
* See modifier key state in the status bar.
* Auto-capitalization of the first letter of a sentence.x	
* Two spaces after a period automatically replaced by a period and a space.

## `sym` modifier map and `emoji` shortcuts

Using the `sym` modifier, you can access more keys and symbols.
For instance, you can use `WASD` (and `HJKL` on the Titan Pocket) to navigate in text.

`sym` modifiers are marked in red; `emoji` modifiers are colored blue (most `emoji` modifiers are yet to be implemented):

![`sym` modifier map MP01](readme-symbehavior-mp01.png)

The Titan Pocket only has a sym layer, though it has more modifier keys available:
![`sym` modifier map Titan Pocket](readme-symbehavior-titanpocket.png)

The Cut/Copy/Paste actions are only available when no modifier is pressed.

## Keyboard Layout

The layouts below are from the original TitanPocketKeyboard project. **Note that the Sym-layer mapping and some long-press characters have been changed in this fork to better suit the Minimal Phone MP01.** The new Sym-layer preview feature is the best way to explore the current layout.


### Accented Characters (Multipress)

You can enable multipress and select a language-specific template for characters accessed via multiple quick presses.


#### French

| Key | Multipress |
| --- | --- |
| `a` | `à`, `â`, `æ` |
| `e` | `é`, `è`, `ê`, `ë` |
| `i` | `î`, `ï` |
| `o` | `ô`, `œ` |
| `u` | `ù`, `û`, `ü` |
| `y` | `ÿ` |
| `c` | `ç` |

#### Spanish

| Key | Multipress |
| --- | --- |
| `a` | `á` |
| `e` | `é` |
| `i` | `í` |
| `o` | `ó` |
| `u` | `ú` |

#### German

| Key | Multipress |
| --- | --- |
| `a` | `ä` |
| `o` | `ö` |
| `u` | `ü` |
| `s` | `ß` |

#### Portuguese

| Key | Multipress |
| --- | --- |
| `a` | `á`, `â`, `à`, `ã` |
| `e` | `é`, `ê` |
| `i` | `í` |
| `o` | `ó`, `ô`, `õ` |
| `u` | `ú` |
| `c` | `ç` |

#### French + ES/DE/PT (default)

| Key | Multipress |
| --- | --- |
| `a` | `à`, `â`, `á`, `ä`, `ã` |
| `e` | `é`, `è`, `ê`, `ë` |
| `i` | `î`, `í`, `ï`, `ì` |
| `o` | `ô`, `ó`, `ò`, `ö`, `õ` |
| `u` | `ù`, `û`, `ú`, `ü` |
| `c` | `ç` |

#### áàâäã

| Key | Multipress |
| --- | --- |
| `a` | `á`, `à`, `â`, `ä`, `ã` |
| `e` | `é`, `è`, `ê`, `ë`, `ẽ` |
| `i` | `í`, `ì`, `î`, `ï`, `ĩ` |
| `o` | `ó`, `ò`, `ô`, `ö`, `õ` |
| `u` | `ú`, `ù`, `û`, `ü`, `ũ` |

#### àáâäã

| Key | Multipress |
| --- | --- |
| `a` | `à`, `á`, `â`, `ä`, `ã` |
| `e` | `è`, `é`, `ê`, `ë`, `ẽ` |
| `i` | `ì`, `í`, `î`, `ï`, `ĩ` |
| `o` | `ò`, `ó`, `ô`, `ö`, `õ` |
| `u` | `ù`, `ú`, `û`, `ü`, `ũ` |

### Additional Characters (after Long Press)


The following table shows the characters that can be accessed with a long press and subsequent multipresses.
The first column is the character printed on the key 

| Key | Long press (Titan) | Long press (MP01) | Following multipresses            |
| --- |--------------------|-------------------|-----------------------------------|
| **`q`** | **`0`**            | `&`               | `°` (degree)                      |
| **`w`** | **`1`**            | `1`               | `&`, `↑`                          |
| **`e`** | **`2`**            | `2`               | `€`, `∃`                          |
| **`r`** | **`3`**            | `3`               | `®`                               |
| **`t`** | **`(`**            | `_`               | `[`, `{`, `<`, `≤`, `†`, `™`      |
| **`y`** | **`)`**            | `-`               | `]`, `}`, `>`, `≥`                |
| **`u`** | **`-`**            | `+`               | `–` (em dash), `–` (en dash), `∪` |
| **`i`** | **`_`**            | `!`               | `\|`                              |
| **`o`** | `ô`                | `#`               | `ó`, `ò`, `ö`, `õ`                |
| **`p`** | **`:`**            | `$`               | `;`, `¶`                          |
| **`a`** | **`@`**            | `@`               | `æ`, `ª`, `←`                     |
| **`s`** | **`4`**            | `4`               | `ß`, `§`, `↓`                     |
| **`d`** | **`5`**            | `5`               | `∂`, `→`, `⇒`                     |
| **`f`** | **`6`**            | `6`               | `^`                               |
| **`g`** | **`*`**            | `=`               | `•`, `·`                          |
| **`h`** | **`#`**            | `:`               | `²`, `♯`                          |
| **`j`** | **`+`**            | `;`               | `=`, `≠`, `≈`, `±`                |
| **`k`** | **`"`**            | `'`               | `%`, `‰`, `‱`                     |
| **`l`** | **`'`**            | `"`               | `` ` ``                           |
| **`z`** | **`!`**            | `7`               | `¡`, `‽`                          |
| **`x`** | **`7`**            | `8`               | `×`, `χ`                          |
| **`c`** | **`8`**            | `9`               | `ç` `©`, `¢`, `⊂`, `⊄`, `⊃`, `⊅`  |
| **`v`** | **`9`**            | `*`               | `∀`, `√`                          |
| **` ` (space bar)** | `	` (tab)| ``                | `⇥`     |
| **`b`** | **`.`**            | `%`               | `…`, `ß`, `∫`, `♭`                |
| **`n`** | **`,`**            | `?`               | `ñ`, `¬`, `∩`                     |
| **`m`** | **`?`**            | `,`               | `$`, `€`, `£`, `¿`                |

# Customizing and contributing

Feel free to adjust the layout to your needs by modifying the code and building your own version. If you think your changes could be useful to others, please consider contributing them back to this project, the original project, or by making a public fork.
