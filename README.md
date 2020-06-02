## Group communication with reliable transmission and universal messaging order

> It was a project assigned to me in the course of distributed systems. The purpose of this work was to build an intermediate software with reliable FIFO multicast operation.
---
**The steps of the project was to build:**
- Build A Group management Thread: Communicates with applications and hold the Groups(Members,Names etc.)
- Build A Fifo reliable Multicast Communication between Apps.


---
## Table of Contents

- [Installation](#installation)
- [Features](#features)

---

## Installation
- All the code required to get started
- Images of what it should look like

### Clone
 - Clone this repo to your local machine using : git clone https://github.com/NickAnge/Messenger.git

### Compile 
  - javac GroupThread.java
  - javac GroupManager.java
  - javac App.java
  - javac Middleware.java
### Start
  - Start GroupManager First: java GroupThread
  - Start Applications one or more (Different Terminal-Windows) : java App
  
  
### Gif-GroupManager
![ttystudio GIF](https://github.com/NickAnge/Messenger/blob/master/src/GroupManager.gif)
### Gif-App
![ttystudio GIF](https://github.com/NickAnge/Messenger/blob/master/src/App.gif)

---
## Features
 - Join Group(One or More)
 - Send A Fifo Message to your Group(Terminal or File)
 - Receive A Fifo Message From your Group (Terminal or File)
