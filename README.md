# About:
JavaCurseClient is a CLI application to download Minecraft modpacks from Curse and to create own modpacks.

# Usage:
Jar need to be executed in a terminal:

java -jar JavaCurseClient.jar [options]

download [Zip File] -- Generate a MultiMC Import for this file

modpack -M [MultiMC Directory] -- Interactive modpack creator

## Modpack downloading:

1. execute java -jar JavaCurseClient.jar download [zip file]
2. JavaCurseClient generates a zip file named Pack Name(MultiMC).zip
3. Open MultiMC -> New instance -> Import modpack -> Pick generated zip file


## Interactive modpack creation:

#### Following command are avalible:

add -- Adds an mod and its required dependencies

remove -- removes a mod

update -- Changes the version of a mod. Defaults to the latest version.

rebuild -- redownload all mods

exit -- quit the program

list -- lists all currently used mods

updateall -- checks for mod updates

#### Mod data format:

When the application asks for the "mod data" it needs the format ProjectID/FileID. example: 233105/2395314 (mcjtylib 2.3.11)

Only declaring the projectID will use the latest version of the mod.

**The projectID can be switched with the project Name.**

It is the last part of the project URL. (https://minecraft.curseforge.com/projects/mcjtylib -> mcjtylib)

#### Examples:

add rftools -- Adds RFTools and all dependencies

remove the-one-probe -- removes The One Probe

update rftools -- updates RFTools to latest version avalible for this Minecraft version

