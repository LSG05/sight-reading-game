# OnSight: A Music Sheet Reading Game
A JavaFX game application that gamifies music sightreading

## Game Mechanics 
A transparent hit-box glides horizontally over a static image of sheet music. The player must press the correct keystrokes as the box passes over the notes. The keyboard controls are the note names C, D, E, F, G, A, and B. 

Scores depend on the correct note and accurate timing of pressing the buttons. A higher combo will lead to a higher multiplier value of each note's points. Stray notes and missed notes are penalized. 

## How it Works
Java and JavaFX are used to build the application. Each song has a hardcoded data of hitbox coordinates, note name, and note timings. The program checks for the accuracy of the keyboard input to the note name and timing. The audio is decoupled through a separate background thread.

## Prerequisites
* at least JDK 21 version
* JavaFX
* IDE (VSCode, IntelliJ)

## Installation and How to Run
1. Download the zip file of the github repository
2. Open the file in your IDE of choice
3. Run the game
```
mvn javafx:run
```

## Developers
* Castro, Maxynne N.
* Gravador, Luke Andre S. 
* Punzal, Myles Dominique B.
