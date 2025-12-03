# QBlackjack

[![Spigot Version](https://img.shields.io/badge/Minecraft-1.21.8-blue.svg)]()
[![License](https://img.shields.io/badge/License-GPL%20v3.0-red.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)

QBlackjack is a dynamic and fully interactive Minecraft plugin that brings the classic casino game of **Blackjack** directly into your server using an immersive chest GUI. It offers a smooth, rule-compliant experience with robust economy integration, allowing players to bet, hit, stand, and win real server currency or dedicated chips.

---

## ✨ Key Features

* **Interactive Chest GUI:** A custom 54-slot GUI (Large Chest) serves as the primary game interface, visually displaying the hands and scores of both the Player and the Dealer.
* **Dual Economy Support:**
    * **Vault Integration:** Uses your server's existing economy (**Vault**) for betting and payouts.
    * **Dedicated Chip System:** A standalone, persistent chip economy can be used as an alternative, isolated currency for gambling.
* **Persistent Chip Data:** Chip balances are safely stored on the server (`ChipManager`) and remain intact across server restarts and player relogs.
* **Standard Blackjack Rules:** Supports core actions including **Hit**, **Stand**, and **Forfeit** (Surrender), complete with automatic checks for Blackjack, Bust, and Push results.
* **Configurable Multipliers:** Payout multipliers for standard wins and Blackjack can be fully customized via the configuration files.
* **Clear Score Display:** Player and Dealer scores are clearly tracked and updated in real-time within the GUI.

---

## 💻 Commands and Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/blackjack <bet_amount>` | Starts a new game of Blackjack with the specified bet amount. | `qblackjack.play` |
| `/blackjack reload` | Reloads the plugin configuration files (`config.yml`, `messages.yml`). | `qblackjack.admin` |
| `/blackjack chip <give/take/set> <player> <amount>` | Admin command to manage player chip balances (**give**, **take**, or **set** the balance). | `qblackjack.admin` |
| `/blackjack help` | Displays the help menu for the plugin. | `qblackjack.help` |

---

## ⚙️ Installation

1.  Download the latest `QBlackjack-v1.21.8.jar` file.
2.  Place the `.jar` file into your server's `plugins` folder.
3.  Ensure you have **Vault** installed if you choose to use the Vault economy mode.
4.  Restart (or reload) your server.
5.  Edit the generated `config.yml` and `messages.yml` files to customize the game and messages.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**. This *copyleft* license ensures that any derivative works based on this code must also be released under the same license and their source code must be made available.

---

## 🤝 Contribution

All contributions are welcome! Feel free to create a **Pull Request** or report bugs via the **Issues** section of this repository.