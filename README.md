# Blink

This game is an experiment in following Material Design practices to create an Android game.

##Objective
The object of the game is to progress you (the ball) as far as possible without hitting any obstacles.

#####Note: This game is designed to support all Android versions from API 18 (Jelly Bean) onwards. At present it's only being tested on a Moto X (2014) and Nexus 7 (2014).

###Noted Bugs / Issues:
* Major thread issue: When thread is stopped, it is sometimes required to be restarted (which causes a crash).
* First wall pair generated is very narrow.
* Shadows aren't drawn correctly.
* Collision detection not 100% (estimation calculated from square around FAB circle).
* No implemented score system yet.
