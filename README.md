# TPAsk

A lightweight and highly customizable plugin for Minecraft Paper 1.21.4 servers, providing simple TPA functionalities.

## Player Features
- **Base TPA Commands**[^1]: `/tpa <player>`, `/tpaccept`, `/tpdeny`, `/tpcancel` - each with aliases.
- **Additional Commands**[^1]: `/tpahere <player>` and `/back` - easily togglable and with aliases.

- **Interactable Chat Messages**: Clickable chat messages that make it easier and quicker to respond to or cancel teleport requests.
- **Usage Penalties**: Configurable in the plugin's `config.yml`, server operators can set hunger penalties to discourage constant TPA usage.
- **Feedback-Friendly**[^2]: Translated user-facing hints, chat messages, `/help` command descriptions and usages along with various sound effects to improve player experience.

## Developer / Operator Features
- **Elevated Privileges**: Server operators can use disabled additional commands.
- **Highly customizable**: Easily change the default plugin behavior by editing the `config.yml` file.
- **Language Packs**: Supports custom language packs, allowing for full translation of the user-facing content.
- **Lightweight and fast**: Small and simple, with no unnecessary overhead, ensuring minimal impact on the server's performance.

## Made With
<p align="left">
  <img src="https://skillicons.dev/icons?i=java,maven" alt="Java and Maven">
</p>

[^1]: *By default, `/tpa` also supports the `/tpahere` functionality via `/tpa <player> <me>`.<br>`/tpa <me> <player>` is also supported and is the same as `/tpa <player>`. This behavior is configurable in **config.yml**.*
[^2]: *English (US) and Serbian (RS) language packs are provided. Custom translations can also be added.*

Made in Apache NetBeans, using **Java** and **Maven** for Minecraft Paper 1.21.4 servers.
> *Although it most likely also works with other 1.21.X versions.*
