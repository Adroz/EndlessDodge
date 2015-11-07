# Blink

* This game is an experiment in following Material Design practices to create an Android game.
* The object of the game is to progress you (the ball) as far as possible without hitting any obstacles.
* The game implements a custom SurfaceView for drawing all game elements and the game engine is contained within a thread. At present the game can be played within global (and social) leaderboards, as well as just locally.

#####Note: This game is designed to support all Android versions from API 18 (Jelly Bean) onwards. At present it's only being tested on a Moto X (2014) ~~and Nexus 7 (2014)~~(No Nexus 7 yet).

###Noted Bugs / Issues:
* Shadows aren't drawn correctly. - Might just leave them out altogether
* Collision detection not 100% (estimation calculated from square around FAB circle).
* Need to adjust what scores are displayed based on login/logout state, and whether sharing scores or not.
* Achievements aren't added yet (only dummies).
* Game doesn't support landscape mode yet.
