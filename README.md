# GetEm

GetEm is a simple game of reverse tag / hide & seek written in Java.


# GAME PLAY

Each player is represented by a colored circle in the game field and
they move the circle around the game field by clicking on the game 
field where they want to move. The player's colored circle will draw
a colored path as they move across the game field to follow the 
location of the mouse click.

One player is designated as the "it" player that everyone must chase
down and attempt to tag. You can tell which player is "it" by the 
color of the GetEm bar above the play field.

Once one of the players tags the "it" player then that player 
becomes "it", the play field is reset, and all the other players 
then begin to chase down the new "it" player.

If a player holds down the Ctrl key on the keyboard while moving 
on the play field their circle will change to invisible and cannot 
be seen on the play field. However, any invisible players who move 
across the colored trails left behind by players then the colored 
trail will be erased and will hint other players to the location 
of the hidden player.


# INSTALLATION

GetEm does not require any special installation steps but it does 
require that Java is installed before you can run GetEm.

Download the getem.jar file to your computer and from a command 
line you can use the following command to run the game.

java -jar getem.jar


# BUILDING

Download the getem source tree to your working directory.

There are two steps required to build getem, the compile step and 
the jar creation step.

To compile getem change your working directory to the source trunk
below the getem directory that contains all the java source files 
and execute the following command.

javac -classpath .:getem/* getem/*.java

You may receive a warning during the compile step but there should
be no errors. If there are errors while compiling then you will
need to address these errors before continuing.

After the compile completes successfully then the jar file for
getem is created by executing the following command.
jar cfm getem.jar Manifest.txt getem/

You should now have the file getem.jar in your source trunk.


# RUNNING

Use the following command to run getem.

java -jar getem.jar


# START SERVER

After running the game you click on the Start Game button to start
the game as a server. Up to 7 other players can then join your 
server over your network connection.

The server uses UDP port 3000 for the server so this port must be 
opened on your firewall and you may need to configure port 
forwarding for UDP 3000 on your router if you are on a shared
Internet connection.


# JOIN SERVER

After running the game you click on the Join Game button to open 
the join game dialog. In the dialog you enter the IP address or 
hostname of the server and a unique player name. Click Ok and 
the game will start if the connection is successful. If the game
does not start then the connection may have failed or the name 
selected is already taken on the server.

