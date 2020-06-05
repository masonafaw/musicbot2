# Submitting issues
When submitting issues please state if you are using the publicly hosted FredBoat or if you are selfhosting.
If you are selfhosting please state which version you are using. You can find the version by running the `;;stats` command.

**If your issue is about an exploit, please email `fred@frederikam.com` about it. Do not publicly disclose exploits.**

# Contributing
FredBoat is built using Gradle, so you should just be able to import the project in your favorite IDE and download all dependencies
while building. Check out the [issues](https://github.com/Frederikam/FredBoat/issues) to find out what needs to be done.

In most cases you will want to submit against the `dev` branch.

## Running the bot
FredBoat relies on a microservice architecture, which means that several tasks have been delegated to other programs:

* FredBoat    - Controls everything, runs commands, queues music, etc
* Sentinel    - Receives events from Discord and forwards them to FredBoat. FredBoat can request more data or submit requests
* Lavalink    - Processes and streams music to Discord voice servers. Controlled by FredBoat
* Quarterdeck - Persistence layer. Handles SQL and caching.
* RabbitMQ    - Brokers messages between FredBoat and Sentinel
* PostgreSQL  - Quarterdeck's database

You will most likely want to work on FredBoat. We use docker-compose to automatically configure and run these services,
with the exception of FredBoat itself, which you will want to run from your IDE. This repository only contains the code
of FredBoat.

### Requirements
* git
* docker
* docker-compose
* Java 10

### Steps
1. Clone this repository - `git clone https://github.com/Frederikam/FredBoat`
2. Enter the config directory - `cd FredBoat/config`
3. Copy the templates into the config directory - `cp templates/* .`
4. Add your bot's token to `common.yml`
5. Start all the services with `docker-compose up -d`

**Protip:** View *all* the logs with `docker-compose logs --follow`

6. Launch FredBoat via your IDE (main file is `fredboat.main.launcher.kt`).

The working directory of FredBoat should be `<repository>/FredBoat`

When you want to stop the services run `docker-compose down`

## Working with Kotlin
FredBoat was initially a java bot, but we are relying incrementally on Kotlin. Kotlin is still very similar to
Java, and it is fairly easy to learn for existing JVM developer. We recommend the [Kotlin Koans](https://kotlinlang.org/docs/tutorials/koans.html)
tutorial to quickly learn many of the basic features.

This project makes heavy use of  [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines.html#coroutines).

We will still accept Java contributions, but many APIs were made with Kotlin syntax in mind.

### Building the project
FredBoat uses Gradle to build the project. FredBoat comes with two gradle wrappers that helps with running Gradle, even if you haven't already installed it. Running it is simple:

```bash
./gradlew clean build -x test

# Or if you use Windows
gradlew.bat clean build -x test
```

We skip the tests because they are more meant to be run on the CI. Your new .jar should be at `<git>/FredBoat/FredBoat.jar`

### Building a Docker image
The project structure does not work particularly well for Docker. To build a custom docker image, set up a directory with these four files:
1. The `Dockerfile` (copy from `<git>/FredBoat/Dockerfile`)
2. Your new `FredBoat.jar` (see above)
2. A default `fredboat.yml`, which is usually overriden
3. A default `common.yml`, which is usually overriden

Then run the command `docker build -t fredboat/fredboat .` Your image will be named `fredboat/fredboat:latest`, and this will be what you put in your `docker-compose.yml` if using docker-compose.

Alternatively, you can add your new jar file to an existing image by using a volume.

## Code conventions
Code is indented with 4 spaces and without brackets on newlines. Please use the logging system (SLF4J) instead of `System.out` or `System.err`.

It is a good practice to make use of the default code formatter of your IDE to adhere to existing conventions of the project. However, please do
not reformat any lines that you aren't actually changing.

### When writing Java
This project makes use of additional annotations to improve quality and cleanliness of the code, most notably `javax.annotation.Nonnull`
& `javax.annotation.Nullable` for parameters and return values of methods. Please use these to describe the contracts of any methods you
are adding. Feel invited to add these when you spot missing ones in the existing code base.

Note that Kotlin's type system make these annotations obsolete, and implicit when working with Java interop.

## Issue labels
* **Beginner** Means that the issue should be suitable for new contributors.
* **Intermediate** Means that the issue might be somewhat more difficult.
* **Advanced** Means that the could be a major feature or requires a deep understanding of the bot.
* **Docs** Means that help is wanted to improve the documentation.

These 4 tags all imply that help is wanted, so feel free to contribute at whichever level.
Please announce your intention to work on something so we don't accidentally try to write the same changes!

Also feel free to work on your own ideas. Especially commands. We can't have too many of those.

## Writing a Command

The easiest way to write a new command for FredBoat from scratch is by having a look at how similar commands are written.
A few core concepts are used by almost every command:

### Extending
A FredBoat command must extend the abstract class
[Command.kt](FredBoat/src/main/java/fredboat/commandmeta/abs/Command.kt).

### Permissions
FredBoat runs its own simplified permission system that takes care of properly interacting with the user.
Existing permission levels are defined in [PermissionLevel.kt](Shared/src/main/java/fredboat/definitions/PermissionLevel.kt), 
and by implementing [ICommandRestricted.kt](FredBoat/src/main/java/fredboat/commandmeta/abs/ICommandRestricted.kt ).
You gain access to a method to provide the minimum required permissions for your command.

### Context
Each command is provided a `CommandContext` during execution. This context provides access to the invoker, guild, textchannel, message,
parsed arguments etc. Further more it provides convenience methods to reply to users.

### Help
Each command is required to provide a proper help string explaining all possible usages and arguments. 
If your command gets too complex for that, adding an additional page to the official documentation
 (example: [Permissions commands](https://fredboat.com/docs/permissions) may be discussed.

Commands intended for public use (read: non-admin, non-maintenance commands) are required to support translation.

### Translations
See [Translations](CONTRIBUTING.md#translations) for more details on our flow of adding
new strings to translation files. You can access translations through any provided `Context`.

### Example: The Shuffle Command

The [ShuffleCommand.java](FredBoat/src/main/java/fredboat/command/music/control/ShuffleCommand.java) makes use of permissions by
implementing `ICommandRestricted` and overriding `getMinimumPerms()` to return a DJ level, making this command only executable by
users with the DJ level or higher. It uses the `CommandContext` to know which guild the command is run in and access that guild's player. 
Being a public command, it uses translated strings in its replies and its help.

<details><summary>Click me</summary>

```java
public class ShuffleCommand extends Command implements IMusicCommand, ICommandRestricted {

    public ShuffleCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);
        player.setShuffle(!player.isShuffle());

        if (player.isShuffle()) {
            context.reply(context.i18n("shuffleOn"));
        } else {
            context.reply(context.i18n("shuffleOff"));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpShuffleCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
```
To be fully usable, the command is registered in one of the CommandInitializers with its name and aliases:
```java
MusicCommandInitializer.java
        [...]
        CommandRegistry.registerCommand(new ShuffleCommand("shuffle", "sh", "random"));
        [...]
```
</details>


## Translations

If you want to fix existing translations, you are welcome to contribute over at [FredBoat's Crowdin project](https://crowdin.com/project/fredboat).

To include new translation strings, add them to the `en_US.properties` file that you can find in the `resources/lang` folder.
 A collaborator will take care of regularly syncing and updating the other translation files through [Crowdin](https://crowdin.com/project/fredboat).

### Rules for translations
- present tense, imperative verbs
- group strings topically, for example help strings belong next to each other in the file
- keep formatting characters (\_ \* \` etc) out of the translations files, they should be added by the code instead
- don't overuse variables, as those make it harder to write good translations


## Setting up your IDE

Obtaining a well functioning and productive setup of the IDE may be an intimidating process, especially for users
 new to coding or the Java world. Here are some tips that will hopefully make this step a bit easier.

### Running the bot

Add the `fredboat.yaml` file to the FredBoat root directory (symlinking recommended).

To run the FredBoat bot in IntelliJ IDEA, find the little green play button in the main class `fredboat.main.Launcher.kt` and start it from there:
<details><summary>Click me</summary>

(Screenshot is from the old main class)

[![Running from IDEA](https://fred.moe/ETs.png)](https://fred.moe/ETs.png)
</details>
<br/>

Alternatively you can set up a gradle task to run or debug FredBoat:
<details><summary>Click me</summary>

[![Edit config of gradle task in IDEA](https://fred.moe/uIk.png)](https://fred.moe/uIk.png)
[![Running with gradle in IDEA](https://fred.moe/iPb.png)](https://fred.moe/iPb.png)
</details>
<br/>

This also allows you to take advantage of Java hotswapping, which you can enable in IDEA like so:
<details><summary>Click me</summary>

[![Hot swapping settings in IDEA](https://fred.moe/XhC.png)](https://fred.moe/XhC.png)
</details>
<br/>

Reloading while the bot is running will have your changes hot swapped, so there is no need to fully
restart it eachtime when you are rapidly testing out a lot of small changes.
<details><summary>Click me</summary>

[![Reloading changed classes in IDEA](https://fred.moe/pFG.png)](https://fred.moe/pFG.png)
</details>
<br/>

### Automating tasks

Reformatting your code, organizing imports, recompiling, etc are tasks that you may end up running _a lot_ each day.

A convenient time to do them is when saving files, so you might want to 
[create a macro](https://www.jetbrains.com/help/idea/using-macros-in-the-editor.html) that does all the housekeeping for you
and put it on the CTRL + S hotkey, or use a plugin like [Save Actions](https://plugins.jetbrains.com/plugin/7642-save-actions).

Here is a decent configuration:
<details><summary>Click me</summary>

[![Save Actions plugin settings](https://fred.moe/j7b.png)](https://fred.moe/j7b.png)
</details>
<br/>

## Writing integration tests
Integration tests are used to verify that production code works as expected, for instance to check if a command returns the expected output.
These tests make it easy to detect when a commit breaks something, and also make it easier to test pull requests before they are merged.

We have developed a special Kotlin DSL (aka fancy API) to deal with testing commands. Here is a simple example:

```kotlin
class SayCommandTest : IntegrationTest() {
    @Test
    fun onInvoke() {
        testCommand(";;say qwe rty") {
            // SayCommand uses an invisible whitespace to not trigger other bots
            assertReply(TextUtils.ZERO_WIDTH_CHAR + "qwe rty")
        }
    }
}
```

The above command emulates that a user invokes the given command. Before this test is executed, a set of Docker services
(Quarterdeck, Lavalink, Postgres) are set up, and the Spring Boot application has been loaded. A fake Sentinel
implementation intercepts all requests so that they can be asserted against. It is also possible to intercept
other requests, like voice join requests, edit requests, etc.

## Join Developer Chat

Are you planning to contribute and have burning questions not answered here?
Please be invited to join [FredBoat Hangout](https://discord.gg/cgPFW4q) and request an admin for writing access to the `#coding` channel.
